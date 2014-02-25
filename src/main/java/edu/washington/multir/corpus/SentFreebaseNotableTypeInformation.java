package edu.washington.multir.corpus;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.util.Triple;

public class SentFreebaseNotableTypeInformation implements SentInformationI{

	public static final class FreebaseNotableTypeAnnotation implements CoreAnnotation<List<Triple<Integer,Integer,String>>>{
		@Override
		public Class<List<Triple<Integer, Integer, String>>> getType() {
			return ErasureUtils.uncheckedCast(List.class);
		}
	}
	@Override
	public void read(String s, CoreMap c) {
		List<Triple<Integer,Integer,String>> notableTypeData = new ArrayList<>();
		String[] data = s.split("\\s+");
		for(int i = 2; i < data.length; i+=3){
			Integer tokenStart = Integer.parseInt(data[i-2]);
			Integer tokenEnd = Integer.parseInt(data[i-1]);
			String type = data[i];
			type = type.replaceAll("__", "_");
			Triple<Integer,Integer,String> t = new Triple<>(tokenStart,tokenEnd,type);
			notableTypeData.add(t);
		}
		c.set(FreebaseNotableTypeAnnotation.class, notableTypeData);
	}

	@Override
	public String write(CoreMap c) {
		List<Triple<Integer,Integer,String>> notableTypeData = c.get(FreebaseNotableTypeAnnotation.class);
		StringBuilder sb = new StringBuilder();
		for(Triple<Integer,Integer,String> notableType : notableTypeData){
			sb.append(notableType.first);
			sb.append(" ");
			sb.append(notableType.second);
			sb.append(" ");
			sb.append(notableType.third);
			sb.append(" ");
		}
		if(sb.length() > 0){
			sb.setLength(sb.length()-1);
		}
		return sb.toString();
	}

	@Override
	public String name() {
		return this.getClass().getSimpleName().toUpperCase();
	}

}
