name := "MultirSystem"

version := "0.1"

scalaVersion := "2.10.1"

fork := true

javaOptions in run += "-Xmx12G"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-ner-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-postag-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-dcoref-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-parse-models" % "1.3.5",
  "edu.washington.cs.knowitall.stanford-corenlp" % "stanford-sutime-models" % "1.3.5",
  "org.apache.derby" % "derby" % "10.10.1.1",
  "edu.washington.cs.knowitall" % "reverb-core" % "1.4.3")
