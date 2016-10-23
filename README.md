# Vandalism Detection (Riberry)
The Riberry Vandalism Detector
Vandalism Detection [Task Description](http://www.wsdm-cup-2017.org/vandalism-detection.html)
 - Server Code https://github.com/wsdm-cup-2017/wsdmcup17-data-server
 - Java Demo https://github.com/wsdm-cup-2017/wsdmcup17-demo-java
 
## Midpoint Report 
Latex https://www.overleaf.com/6697439jbqvdt

## Reference github
Feature Extraction
https://github.com/wsdm-cup-2017/wsdmcup17-wdvd-baseline-feature-extraction

Logistic regression
https://github.com/tpeng/logistic-regression

Random Forest
https://github.com/ironmanMA/Random-Forest

GBDT (gradient boosting decision tree)
https://github.com/dmlc/xgboost


To run server demo:

java -jar wsdmcup17-data-server-0.0.1-SNAPSHOT-jar-with-dependencies.jar -r /home/yiran/wsdm-data/wdvc16_2013_01.xml.7z -m /home/yiran/wsdm/wdvc16_meta.csv.7z -o /home/yiran/wsdm/serverout -p 8888

To run client demo:

java -jar wsdmcup17-demo-java-0.0.1-SNAPSHOT-jar-with-dependencies.jar -s localhost:8888 -t 123456

Note jar files are in github target folders. 7z files should be in your own computer. 

