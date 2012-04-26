(ns grid-jenkins.core
  (:use [overtone.device protocols launchpad])
  (:require [clj-json.core :as json]
            [clj-http.client :as http]
            [overtone.libs.handlers :as handlers]
            [clojure.java.browse :as browse]))

(defonce lp (make-launchpad))

(def jenkins-status (json/parse-string (slurp "/Users/philippotter/example-jenkins-build.json")))

(defonce button-state (atom {}))

(def jenkins-base-url "http://jenkins/")

(defn json-url [url]
  (if (= \/ (last (seq url)))
    (str url "api/json")
    (str url "/api/json")))

(def color
  {"red"        1
   "red_anime"  1
   "yellow"     1
   "yellow_anime" 1
   "blue"       2
   "blue_anime" 2
   "grey"       3
   "grey_anime" 3
   "disabled"   3
   })

(defn update-lp [lp jobs]
  (doseq [y (range 8)
          x (range 8)]
    (if-let [job (get @button-state [x y])]
      (let [colour (get color (get job :color) 1)]
        (when (not (contains? color (get job :color)))
          (print "don't understand color from" job))
        (light-colour lp x y colour))
      (light-colour lp x y 0))))

(defn update-state [lp jobs]
  (reset! button-state (zipmap (for [y (range 8) x (range 8)] [x y]) jobs))
  (update-lp lp jobs))

(defonce a (agent nil))

(defn timer-loop [_]
  (let [jenkins-status-response (http/get (json-url @#'jenkins-base-url) {:as :json})
        jobs (get-in jenkins-status-response [:body :jobs])]
    (update-state lp jobs)
    (Thread/sleep (* 10 6000))
    (send-off *agent* timer-loop)
    jenkins-status-response))

(defn key-event [event]
  (when (= (:event event) :press)
    (let [key (:key event)]
      (when-let [url (get-in @button-state [key :url])]
        (browse/browse-url url)))))

(defn start []
  (handlers/add-handler! (:handler-pool lp) :launchpad-key :jenkins key-event)
  (send-off a timer-loop))

