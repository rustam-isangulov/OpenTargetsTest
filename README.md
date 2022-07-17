# Open Targets Test
This project is a solution for Open Targets Developer Technical Test. Objectives for the test are described [here](../main/documents/ebi01989_software_developer_-_take_home_tech_test.pdf).

# Results

|Objective|Results|
|----|----|
|`...generate the overall association score for a given target-disease association.` | number of overall association scores: **_25,132_**; <br />resulting table in json format is in **_./output_** directory |
|`Count how many target-target pairs share a connection to at least two diseases.` | Number of target-target pairs with at least 2 shared connections: **_350,414_** |

# Reproduce results with precompiled jars
1. clone this repository

```shell
git clone https://github.com/rustam-isangulov/OpenTargetsTest.git
```
2. change directory to `OpenTargetsTest`

```shell
cd OpenTargetsTest
```

# Compile source code
