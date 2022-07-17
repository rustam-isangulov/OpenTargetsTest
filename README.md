# Open Targets Test
This project is a solution for an Open Targets Developer Technical Test. Objectives for the test are described [here](../main/documents/ebi01989_software_developer_-_take_home_tech_test.pdf).

# Results

|Objective|Results|
|----|----|
|`...generate the overall association score for a given target-disease association.` | number of overall association scores: **_25,132_**; <br />resulting table in json format is in **_./output_** directory |
|`Count how many target-target pairs share a connection to at least two diseases.` | Number of target-target pairs with at least 2 shared connections: **_350,414_** |

# Reproduce results with precompiled jars
## System requirements
Java 11
#### tested environments
macos:
```shell
java -version
openjdk version "11.0.12" 2021-07-20
OpenJDK Runtime Environment Homebrew (build 11.0.12+0)
OpenJDK 64-Bit Server VM Homebrew (build 11.0.12+0, mixed mode)
```

## Steps
1. clone this repository

```shell
git clone https://github.com/rustam-isangulov/OpenTargetsTest.git
```
2. change directory to `OpenTargetsTest`

```shell
cd OpenTargetsTest
```
3. download data

```shell
java -jar bin/ftputil.jar -s "ftp.ebi.ac.uk" -r "/pub/databases/opentargets/platform/21.11/output/etl/json/" -l "./data/" -d "evidence/sourceId=eva/"
```

```shell
java -jar bin/ftputil.jar -s "ftp.ebi.ac.uk" -r "/pub/databases/opentargets/platform/21.11/output/etl/json/" -l "./data/" -d "diseases/"
```

```shell
java -jar bin/ftputil.jar -s "ftp.ebi.ac.uk" -r "/pub/databases/opentargets/platform/21.11/output/etl/json/" -l "./data/" -d "targets"
```

4. process data to produce results

```shell
java -jar bin/overallscore.jar -e "./data/evidence/sourceId=eva/" -t "./data/targets/" -d "./data/diseases/" -o "./output"
```

<details><summary>expected output</summary>
```shell
Proceeding with the following parameters
	Evidence path: [./data/evidence/sourceId=eva]
	Targets path: [./data/targets]
	Diseases path: [./data/diseases]
	Output path: [./output]
	Min number of shared connections: [2]

Start processing evidence data...
Total elapsed time for the evidence data processing: 2559 (ms)
Number of target-disease overall association scores: 25132

Start processing target data...
Total elapsed time for the target data processing: 1879 (ms)
Number of targets: 60636

Start processing disease data...
Total elapsed time for the disease data processing: 112 (ms)
Number of diseases: 18706

Start processing joint Association/Target/Disease data set...
Total elapsed time for the joint Association/Target/Disease data set processing: 141 (ms)
Number of overall association scores: 25132

Start processing targets with shared disease connections...
Total elapsed time for the targets with shared disease connections processing: 958 (ms)
Number of target-target pairs with at least 2 shared connections: 350414
```

Resulting joint dataset is exported to _./output_ directory.


# Compile source code
