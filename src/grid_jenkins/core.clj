(ns grid-jenkins.core
  (:require [clj-http.client :as http]
            [coremidi-clj.coremidi :as coremidi]
            [coremidi-clj.coremidi.native :as coremidi.native]
            [clojure.java.browse :as browse]))

(defonce lp-out (coremidi/midi-out "Launchpad"))

(defonce button-state (atom {}))

(defonce http-agent (agent nil))

(def jenkins-base-url "http://ci.jruby.org/")

(defn json-url [url]
  (if (= \/ (last (seq url)))
    (str url "api/json")
    (str url "/api/json")))

(def default-color
  {"red"        :red
   "red_anime"  :red-dim
   "yellow"     :yellow
   "yellow_anime" :yellow-dim
   "blue"       :green
   "blue_anime" :green-dim
   "grey"       :off
   "grey_anime" :off
   "disabled"   :off
   })

(def color-map
  {:red        4r033
   :red-dim    4r031
   :yellow     4r333
   :yellow-dim 4r131
   :green      4r330
   :green-dim  4r130
   :off        4r030})

(defn lp-color [jenkins-color]
  (color-map (get default-color jenkins-color :off)))

(defn coords->midi-note [x y]
  (+ x (* 16 y)))

(defn light-colour [lp x y colour]
  (coremidi/midi-note-on lp (coords->midi-note x y) colour))

(defn update-lp [lp jobs]
  (doseq [y (range 8)
          x (range 8)]
    (if-let [job (get @button-state [x y])]
      (light-colour lp x y (lp-color (:color job)))
      (light-colour lp x y 0))))

(defn update-state [lp jobs]
  (reset! button-state (zipmap (for [y (range 8) x (range 8)] [x y]) jobs))
  (update-lp lp jobs))

(defn poll-jenkins-loop [_]
  (let [jenkins-status-response (http/get (json-url @#'jenkins-base-url) {:as :json})
        jobs (get-in jenkins-status-response [:body :jobs])]
    (update-state lp-out jobs)
    (Thread/sleep (* 10 6000))
    (send-off *agent* poll-jenkins-loop)
    jenkins-status-response))

(defn key-event [event]
  (when (= (:event event) :press)
    (let [key (:key event)]
      (when-let [url (get-in @button-state [key :url])]
        (browse/browse-url url)))))

(defn start []
  #_(handlers/add-handler! (:handler-pool lp-in) :launchpad-key :jenkins key-event)
  (send-off http-agent poll-jenkins-loop))

