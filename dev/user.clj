(ns user
  (:require
   [figwheel-sidecar.repl-api :as f]))

(defn fig-start
  "This starts the figwheel server and watch based auto-compiler."
  []
  (f/start-figwheel!))

(defn fig-stop
  "Stop the figwheel server and watch based auto-compiler."
  []
  (f/stop-figwheel!))

(defn cljs-repl
  "Launch a ClojureScript REPL that is connected to your build and host environment."
  []
  (f/cljs-repl))