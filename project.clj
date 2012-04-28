(defproject grid-jenkins "0.0.1-SNAPSHOT"
  :description "Use a grid-type device as a Jenkins build monitor"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [overtone/device.protocols "0.0.4-SNAPSHOT"]
                 [overtone/device.launchpad "0.0.5-SNAPSHOT"]
                 [overtone/libs.handlers "0.2.0-SNAPSHOT"]
                 [clj-http "0.4.0"]])