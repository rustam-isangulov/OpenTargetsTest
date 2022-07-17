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
#### tested environements
macos:
```shell
java -version
openjdk version "11.0.12" 2021-07-20
OpenJDK Runtime Environment Homebrew (build 11.0.12+0)
OpenJDK 64-Bit Server VM Homebrew (build 11.0.12+0, mixed mode)
```
ubuntu:
```shell
java -version
openjdk version "11.0.15" 2022-04-19
OpenJDK Runtime Environment (build 11.0.15+10-Ubuntu-0ubuntu0.20.04.1)
OpenJDK 64-Bit Server VM (build 11.0.15+10-Ubuntu-0ubuntu0.20.04.1, mixed mode, sharing)
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

3. process data to produce results

```shell
java -jar bin/overallscore.jar -e "./data/evidence/sourceId=eva/" -t "./data/targets/" -d "./data/diseases/" -o "./output"
```

Resulting joint dataset is exported to _./output_ directory.


# Compile source code
