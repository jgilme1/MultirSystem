package edu.washington.multir.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.cs.knowitall.util.HtmlUtils;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.stanford.nlp.trees.Tree;


public class CorpusPreprocessing {
	private static String options = "invertible=true,ptb3Escaping=true";
	private static Pattern ldcPattern = Pattern.compile("<DOCID>\\s+.+LDC");
	private static Pattern xmlParagraphPattern = Pattern.compile("<P>((?:[\\s\\S](?!<P>))+)</P>");
	private static LexedTokenFactory<CoreLabel> ltf = new CoreLabelTokenFactory(true);
	private static WordToSentenceProcessor<CoreLabel> sen = new WordToSentenceProcessor<CoreLabel>();
	private static Properties props = new Properties();
	private static StanfordCoreNLP pipeline;
	private static TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	private static GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

	public static void main(String[] args) throws IOException, InterruptedException{

		
		props.put("annotators", "pos,lemma,ner");
		props.put("sutime.binders","0");
		props.put("ner.useSUTime", "false");
		pipeline = new StanfordCoreNLP(props,false);

		String docPath = args[0];
		String documentString = FileUtils.readFileToString(new File(docPath));

		List<String> paragraphs = cleanDocument(documentString);
		List<CoreMap> sentences = new ArrayList<CoreMap>();
		
		String[] docSplit = docPath.split("/");
		String docName = docSplit[docSplit.length-1].split("\\.")[0];
		
		File cjInputFile = File.createTempFile(docName, "cjinput");
		File cjOutputFile = File.createTempFile(docName, "cjoutput");
		cjOutputFile.deleteOnExit();
		cjInputFile.deleteOnExit();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(cjInputFile));
		
		for(String par: paragraphs){
			par = cleanParagraph(par);
			
			//tokenize
			PTBTokenizer<CoreLabel> tok = new PTBTokenizer<CoreLabel>(
					new StringReader(par), ltf, options);
			List<CoreLabel> l = tok.tokenize();
			List<List<CoreLabel>> snts = sen.process(l);
			
			//process each sentence 
			for(List<CoreLabel> snt: snts){
				//get snt original text
				String sentenceText = getSentenceTextAnnotation(snt,par);
				Annotation sentence = new Annotation(sentenceText);
				
				//set tokens on Annotation sentence
				sentence.set(CoreAnnotations.TokensAnnotation.class, snt);
				
				//get String for tokens separated by white space
				StringBuilder tokensBuilder = new StringBuilder();
				for(CoreLabel token: snt){
					token.set(CoreAnnotations.TokenBeginAnnotation.class, token.beginPosition());
					token.set(CoreAnnotations.TokenEndAnnotation.class, token.endPosition());
					tokensBuilder.append(token.value());
					tokensBuilder.append(" ");
				}
				String tokenString = tokensBuilder.toString().trim();
				
				//preprocess sentence text for charniak-johnson parser
				String cjPreprocessedString = cjPreprocessSentence(tokenString);
				bw.write(cjPreprocessedString +"\n");
				
				sentences.add(sentence);
			}
		}
		Annotation doc = new Annotation(sentences);
		//get pos and ner information from stanford processing
		pipeline.annotate(doc);		
		bw.close();
		
		//run charniak johnson parser
		File parserDirectory = new File("/scratch2/code/JohnsonCharniakParser/bllip-parser/");
		ProcessBuilder pb = new ProcessBuilder();
		List<String> commandArguments = new ArrayList<String>();
		commandArguments.add("./parse.sh");
		pb.command(commandArguments);
		pb.directory(parserDirectory);
		pb.redirectInput(cjInputFile);
		pb.redirectOutput(cjOutputFile);
		pb.redirectError(new File("test.err"));
		Process p =pb.start();
		p.waitFor();
		
		
		
		//read cj parser output and run stanford dependency parse
		BufferedReader in = new BufferedReader(new FileReader(cjOutputFile));
		String nextLine;
		int index =0;
		while((nextLine = in.readLine()) != null){
			//initialize custom Dependency Parse Structure
			List<Triple<Integer,String,Integer>> dependencyInformation= new ArrayList<>();
			
			//put parse information in a tree and get dependency parses
			Tree parse = Tree.valueOf(nextLine.replaceAll("\\|", " "));
			GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
			Collection<TypedDependency> tdl = null;
			try {
				tdl = /*gs.allTypedDependencies();*/ gs.typedDependenciesCCprocessed();
			} catch (NullPointerException e) {
				// there has to be a bug in EnglishGrammaticalStructure.collapseFlatMWP
				tdl = new ArrayList<TypedDependency>();
			}
			
			//convert dependency information into custom annotation
			List<TypedDependency> l = new ArrayList<TypedDependency>();
			l.addAll(tdl);
			for (int i=0; i < tdl.size(); i++) {
				TypedDependency td = l.get(i);
				String name = td.reln().getShortName();
				if (td.reln().getSpecific() != null)
					name += "-" + td.reln().getSpecific();
				Integer governor = td.gov().index();
				String type = name;
				Integer child = td.dep().index();
				Triple<Integer,String,Integer> t = new Triple<>(governor,type,child);
				dependencyInformation.add(t);

			}
			
			//set annotation on sentence
			CoreMap sentence = sentences.get(index);
			sentence.set(DefaultCorpusInformationSpecification.SentDependencyInformation.DependencyAnnotation.class,
					dependencyInformation);
			
			index++;
		}
		in.close();

	}

	private static String cjPreprocessSentence(String sentenceTokensText) {
		String[] toks = sentenceTokensText.split(" ");
		if (toks.length <= 120) {
			return "<s> " +sentenceTokensText+ " </s>\n";
		}
		else{
			return "\n";
		}
	}

	private static String cleanParagraph(String par) {
        par
        // replace urls
		.replaceAll("https?://\\S+?(\\s|$)", "U_R_L$1")
		// replace emails
		.replaceAll(
				"[A-Za-z0-9\\.\\-]+?@([A-Za-z0-9\\-]+?\\.){1,}+(com|net)",
				"E_M_A_I_L")
		// replace "<a ... @xxx.yyy>" emails
		.replaceAll(
				"<[A-Za-z0-9\\.\\-]+? [\\.]{3} @([A-Za-z0-9\\-]+?\\.){1,}+(com|net)>",
				"E_M_A_I_L")
		// replace long dashes
		.replaceAll("[\\-_=]{3,}+", "---")
		// replace all utf8 spaces to the simplest space
		.replaceAll("\\s+", " ")
		// e.g. "</a" is left as a token due to bad html writing
		.replaceAll("</\\p{Alnum}", "");
        par = HtmlUtils.removeHtml(par).replace("[ \t\\u000B\f\r]+", " ");
		
		return par;
	}

	private static String getSentenceTextAnnotation(List<CoreLabel> snt, String par) {
		int sntBegin = snt.get(0).beginPosition();
		return par.substring(sntBegin, snt.get(snt.size() - 1)
				.endPosition());
	}

	private static List<String> cleanDocument(String documentString) {
		Matcher m = ldcPattern.matcher(documentString);
		if(m.find()){
			return getXMLParagraphs(documentString);
		}else{
			return getParagraphs(documentString);
		}
	}

	private static List<String> getParagraphs(String documentString) {
		List<String> paragraphs = new ArrayList<String>();
		String[] ps = documentString.split("\\n{2,}");
		for(String p : ps){
			paragraphs.add(p);
		}
		return paragraphs;
	}

	private static List<String> getXMLParagraphs(String documentString) {
		Matcher m = xmlParagraphPattern.matcher(documentString);
		List<String> paragraphs = new ArrayList<String>();
		while(m.find()){
			String paragraph = m.group(1);
			paragraphs.add(paragraph);
		}
		return paragraphs;
	}
}
