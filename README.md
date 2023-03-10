# corpuscula

A single-page tool for formatting Russian National Corpus data into Russian Wiktionary templates.

## Usage
![usage example](https://user-images.githubusercontent.com/111509227/224373062-9407f310-b794-4c2a-bc9f-50a918f98494.gif)

## Setup

To get an interactive development environment run:


    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2023 esbudylin

Distributed under the Eclipse Public License either version 2.0 or (at your option) any later version.
