# Readme

Version 2.0.0-M6-public

## About

This is the sixth milestone release of version 2.0. For publication, some contents have been removed, for example support of OWL which is under heavy development right now.

This repository contains the reference implementation for the subject-oriented modeling language ["Parallel Activity Specification Scheme" (PASS)](https://github.com/I2PM/PASS-Standard-Book-Tex-Project).

See TODO.md for proposed features and known problems.

## Execution

This project is developed using OpenJDK 11. It is recommended to use Java 11 or newer. You can check your Java version by typing `java -version` in a terminal / Command Prompt. There are no other requirements as everything is provided in the jar files.

The execution is separated into two programs. The interpreter which runs in the background and a console application which loads the processes and controls the execution.

Both applications communicate via a fault-tolerant Akka connection. This allows restarting/stopping them independently.

### Process interpreter

Windows: to start the process interpreter open a Command Prompt, change to this directory and run `start_semantic.cmd`.

Unix: to start the process interpreter open a terminal, change to this directory and run `./start_semantic.sh`. You may need to set the file executable before that via `chmod +x start_semantic.sh`.

The process interpreter will print for each execution step debugging information. As the interpretation happens in an endless loop this will be very noisy - but you can leave this terminal / Command Prompt in the background as you control everything with the console application.


You can terminate the process interpreter with CTRL+C. On Unix-like systems you can pause it with CRTL+Z and resume it with `fg`.


### Console application

Windows: to start the console application open a second Command Prompt, change to this directory and run `start_console.cmd`.

Unix: to start the console application open a second terminal, change to this directory and run `./start_console.sh`. You may need to set the file executable before that via `chmod +x start_console.sh`.

In the beginning you will have no available actions as no processes are running. You can always hit the enter key to trigger a reload of the available actions.

You can exit the Console Application with `quit` and print a short help with `help`.


### Process execution

Processes can be loaded with the Console Application which parses them and sends them to the process interpreter.

To load a process type (for example) `process load ./processes/template.pass` into the Console Application.

To start a new process instance of a loaded process type (for example) `process start Template` into the Console Application.

You can manually force the termination of a process instance with (for example) `process kill 1`.

You can quit the Console Application with `quit`.

All commands support tab-completion, especially file name completion.

You can edit the provided application.conf file, for example to change the ports of the Akka systems if the default ports are already in use, or to adjust the debugging level and logfile.


### Example Process Models

Example processes are given in the `processes` folder.

## Process Model Parsing

If you don't want to execute Process Models, but just want to parse and print them
into the case classes provided in the package `de.athalis.pass.processmodel.tudarmstadt`,
you can use the Process Parsing Util
and don't need to start the UI nor the Process interpreter.
It takes a single path to a process file as argument.

For example, run in a console `parse_processfile.cmd processes\Macro.graphml` on Windows
or `./parse_processfile.sh processes/Macro.graphml` on Unix.

You can parse multiple files at once, which might be required if a process is distributed
across multiple files, if you separate the files with the system path separator.
For example on Windows: `parse_processfile.cmd processes\Macro.graphml;processes\echo_server.pass`
For example on \*nix: `./parse_processfile.sh processes\Macro.graphml:processes\echo_server.pass`


## Literature

* André Wolski. **Spezifikation einer Ausführungssemantik für das Subjektorientierte Prozessmanagement mit CoreASM**. Bachelor-Thesis, TU Darmstadt, October 2018. https://tuprints.ulb.tu-darmstadt.de/id/eprint/8360
* André Wolski, Stephan Borgert, and Lutz Heuser. **An extended Subject-Oriented Business Process Management Execution Semantics**. In _S-BPM ONE 2019, June 26 - 28, 2019, Seville, Spain_. ACM, New York, NY, USA, 2019. https://doi.org/10.1145/3329007.3332706
* André Wolski, Stephan Borgert, and Lutz Heuser. **A CoreASM based Reference Implementation for Subject-Oriented Business Process Management Execution Semantics**. In _S-BPM ONE'19, June 26 - 28, 2019, Seville, Spain_. ACM, New York, NY, USA, 2019. https://doi.org/10.1145/3329007.3329018
* Matthes Elstermann, and André Wolski. **Mapping Execution and Model Semantics for Subject-Oriented Process Models**. In: _Freitag, M., Kinra, A., Kotzab, H., Kreowski, HJ., Thoben, KD. (eds) Subject-Oriented Business Process Management. The Digital Workplace – Nucleus of Transformation._ S-BPM ONE 2020. Communications in Computer and Information Science, vol 1278. Springer, Cham. http://doi.org/10.1007/978-3-030-64351-5_4
* **PASS Standard Book** (work in progress): https://github.com/I2PM/PASS-Standard-Book-Tex-Project


## Development

See DEVELOPMENT.md


## Contact

André Wolski <andre.wolski@stud.tu-darmstadt.de>
