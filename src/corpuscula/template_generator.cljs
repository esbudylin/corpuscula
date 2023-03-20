(ns corpuscula.template-generator
  (:require [clojure.string :as str]))

(def author-name "[А-Я][а-я]+(\\-[А-Я][а-я]+)?")

(def author-initial "[А-Я](?:[а-я]{1,2})?\\.")

(def author-regex
  (re-pattern (str
               "^( ?(?:" author-initial " ?(?:" author-initial ")? " author-name "),?"
               "|( ?(?:" author-name " " author-initial "(?: ?" author-initial ")?)),?"
               "|(( ?(" author-name ")){2,3})"
               "|( ?[а-я]+ " author-name " \\(" author-name "\\))" ; e.g. архиепископ Иннокентий (Борисов)
               ",?)+")))

(def publisher-regex
  #"«(.*?)»| ([a-zA-Z0-9А-Я \.\-—:\?]+), ")

(def date-regex
  #"\(([0-9-—.]+)\)")

(def publication-date-regex
  #"([0-9\-—.]+)")

(defn title-regex [author date]
  (re-pattern
   (apply str (flatten
               [(when author (str/replace author #"[\(\)]" #(str "\\" %)))
                "(?:\\.| )?(.*?)"
                (if date [" \\(?" date] "$")]))))

(defn find-highlighted-group [text highlights]
  (loop [text text acc [] to-test []]
    (if (empty? text) acc
        (let [[i snippet] (first text)]
          (cond
            (contains? highlights i) (recur (rest text) (conj acc to-test snippet) [])
            (not (re-matches #"[А-Яа-я\w]+|\." snippet)) (recur (rest text) acc (conj to-test snippet))
            :else acc)))))

(defn highlight-examples [text highlights opening closing]
  (loop [text (map-indexed vector text) acc []]
    (if (empty? text) acc
        (let [[i snippet] (first text)]
          (if (contains? highlights i)
            (let [highlighted-group (flatten (find-highlighted-group text highlights))]
              (recur (drop (count highlighted-group) text) (conj acc [opening highlighted-group closing])))
            (recur (rest text) (conj acc snippet)))))))

(defn match-numeric-month [numeric-month]
  (case numeric-month
    "01" "января"
    "02" "февраля"
    "03" "марта"
    "04" "апреля"
    "05" "мая"
    "06" "июня"
    "07" "июля"
    "08" "августа"
    "09" "сентября"
    "10" "октября"
    "11" "ноября"
    "12" "декабря"
    nil))

(defn format-month [split-date]
  (let [numeric-month (when (> (count split-date) 2) (second split-date))
        cyrillic-month (match-numeric-month numeric-month)]
    (when numeric-month
      (str/join " " [(str/replace (nth split-date 0) #"^0" "") cyrillic-month (nth split-date 2)]))))

(defn format-date [date add-params?]
  (let [split-date (str/split date #"\.")
        formatted-month (format-month split-date)
        date (if formatted-month formatted-month date)
        years-appended (if (and add-params? (re-find #"[\-–]" (last split-date))) (str/join " " [date "гг."]) date)]
    (when years-appended (str/replace years-appended "-" "–"))))

(defn handle-title [example-title add-params?]
  (let [[work-title publisher-data] (str/split example-title #"//")
        author (first (re-find author-regex work-title))
        date (last (last (re-seq date-regex work-title)))
        title (last (re-find (title-regex author date) work-title))
        [publication-date publisher] (when publisher-data
                                       [(last (last (re-seq publication-date-regex publisher-data)))
                                        (last (filter some? (re-find publisher-regex publisher-data)))])]
    [author (str/trim title) (format-date date add-params?) publisher (format-date publication-date add-params?)]))

(def param-names ["автор" "титул" "дата" "издание" "дата издания"])

(defn generate-template [title text highlights extra-params]
  (let [text (apply str (flatten (highlight-examples text highlights "{{выдел|", "}}")))
        values (handle-title title (extra-params :add-params?))]
    (apply str (flatten
                ["{{пример|" text
                 (if (extra-params :add-params?)
                   (for [[name value] (zipmap param-names values) :when value]
                     ["|" name "=" value])
                   (for [[i, value] (map-indexed vector values) :while (some some? (drop i values))]
                     ["|" value]))
                 (when (extra-params :rnc?) "|источник=НКРЯ")
                 (when (extra-params :without-quotes?) "|бк=1")
                 (when (extra-params :publisher-without-quotes?) "|издание без кавычек=1") "}}"]))))