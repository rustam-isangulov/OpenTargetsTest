# Open Targets Test
This project is a solution for an Open Targets Developer Technical Test. Objectives for the test are described [here](../main/documents/ebi01989_software_developer_-_take_home_tech_test.pdf).

# Results

|Objective|Results|
|----|----|
|`...generate the overall association score for a given target-disease association.` | number of overall association scores: **```25,132```**; <br />resulting table in json format is in **```./output```** directory |
|`Count how many target-target pairs share a connection to at least two diseases.` | Number of target-target pairs with at least 2 shared connections: **```350,414```** |

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
<p>

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
</p>
</details>

Resulting joint dataset is exported to _./output_ directory.


# Command line interface

#### ```ftputil```

```shell
usage: java -jar ftputil.jar -d <dir> -l <local_dir> -r <remote_dir> -s
       <ftp_address>

Download files from a directory on an ftp server

Options:
 -d,--dir <dir>                directory to download files from (relative
                               to remotedir) and to (relative to localdir)
 -l,--localdir <local_dir>     local base directory
 -r,--remotedir <remote_dir>   remote base directory
 -s,--server <ftp_address>     remote ftp server uri

Example:
 java -jar ftputil.jar -s "ftp.ebi.ac.uk" -r
"/pub/databases/opentargets/platform/21.11/output/etl/json/" -l "./data/"
-d "evidence/sourceId=eva/"
```

#### ```overallscore```

```shell
usage: java -jar overallscore.jar -d <diseases_dir> -e <evidence_dir> -o
       <output_dir> [-sn <number>] -t <targets_dir>

Generate the overall association scores for given target-disease
associations and Calculate the number of target-target pairs that share a
connection to a specified number of diseases.

Options:
 -d,--diseases <diseases_dir>   directory that contains diseases *.json
                                files
 -e,--evidence <evidence_dir>   directory that contains evidence *.json
                                files
 -o,--output <output_dir>       directory for the overall association
                                scores output *.json file
 -sn,--sharednumber <number>    min number of shared diseases for
                                target-target shared connection statistics
 -t,--targets <targets_dir>     directory that contains targets *.json
                                files

Example:
 java -jar overallscore.jar -e "./evidence/sourceId=eva/" -t "./targets/"
-d "./diseases/" -o "./output/" -sn 2
```

---
