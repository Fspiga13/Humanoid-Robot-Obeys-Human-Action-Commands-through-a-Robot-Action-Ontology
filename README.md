# Humanoid Robot Obeys Human Action Commands through a Robot Action Ontology

## Getting started
### Prerequisites
Before to use the application, you need to download the following packages:
```
pip install requests
pip install hdfs
pip install bs4
pip install rdflib
pip install nltk
pip install graphviz
pip install stanfordcorenlp
```
### Installing
Download [Stanford coreNLP](https://stanfordnlp.github.io/CoreNLP/) and move Stanford main folder into the TextToRDF folder.
To start Stanford coreNLP locally and listening on port 9000 run the following command after moving to the main Stanford folder:
(Remember that using Stanford locally requires about 4GB of RAM space)
```
cd /ZoraNlpReasoner/TextToRDF/stanford-corenlp-full-2018-10-05/
java --add-modules java.se.ee -mx4g -cp "*" edu.stanford.nlp.pipeline.StanfordCoreNLPServer -port 9000 -timeout 15000
```
After running Stanford, you can test the operation of the RDF creation tool from the Stanford output. Run the following command after moving to the src folder, TextToRDF subfolder:
```
cd /ZoraNlpReasoner/TextToRDF/src/
python xproject.py -t "<sentence>"
```





## Running tests


## Repository contents
### IncompatibilityTable
The table shows for each action in the ontology the actions that cannot precede it and therefore are incompatible. Actions that have no incompatibility can always be performed.
