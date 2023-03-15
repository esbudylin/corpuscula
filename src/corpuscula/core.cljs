(ns corpuscula.core
  (:require corpuscula.template-generator goog.object
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [reagent.dom :as rd]))

(enable-console-print!)

(def initial-data
  {:title ""
   :text ""
   :idxs-to-highlight #{}})

(defonce text-data (atom initial-data))

(defonce current-screen (atom 0))

(def max-screen 2)

(def clipboard
  (-> js/navigator
      (goog.object/get "clipboard")))

(def write-text-into-clipboard
  (.bind (goog.object/get clipboard "writeText") clipboard))

(defn change-screen-button [text change update-func disabled?]
  [:input {:type "button" :disabled disabled? :value text
           :on-click (fn [] (swap! current-screen change) (when update-func (update-func)))}])

(defn change-screen-panel [update-func]
  [:div.bottomleft [change-screen-button "Назад" dec update-func (= @current-screen 0)]
   (if (= @current-screen max-screen)
     [change-screen-button "В начало" (partial * 0) #(reset! text-data initial-data)]
     [change-screen-button "Вперёд" inc update-func])])

(defn text-input [value intro-text id enter-func multiline? input-check]
  [:div
   [:p intro-text]
   [:textarea
    {:value @value
     :id id
     :on-change (fn [e]
                  (reset! value (str/replace (-> e .-target .-value) #"\n" ""))
                  (when input-check (input-check)))
     :on-key-press #(when (= 13 (.-charCode %)) (enter-func))
     :rows (if multiline? 5 3)}]])

(defn input-check [value-to-check value-to-update id-to-focus]
  (let [finds (re-find #" \[(.*)\]$" @value-to-check)
        [to-erase to-add] finds
        erase #(apply str (drop-last (count to-erase) %))
        update-values (fn []
                        (swap! value-to-check erase)
                        (reset! value-to-update to-add)
                        (.focus (.getElementById js/document id-to-focus)))]
    (when finds (update-values))))

(defn text-input-screen []
  (let [text (reagent/atom (@text-data :text))
        title (reagent/atom (@text-data :title))
        swap-values (fn []
                      (when (not= @text (@text-data :text))
                        (swap! text-data assoc :idxs-to-highlight #{})
                        (swap! text-data assoc :text @text))
                      (swap! text-data assoc :title @title))]
    [:div
     [text-input text "Введите текст:" "text"
      #(.focus (.getElementById js/document "title")) true #(input-check text title "title")]
     [text-input title "Введите заголовок:" "title"
      (fn []
        (swap-values)
        (when (< @current-screen max-screen) (swap! current-screen inc)))]
     [change-screen-panel #(swap-values)]]))

(defn word-to-choose [t highlighted]
  (if (re-matches #"[А-Яа-я\w]+" t)
    [:span.pointer {:style (when @highlighted {:font-weight "bold"})
                    :on-click #(swap! highlighted not)} t]
    [:span t]))

(defn words-to-highlight-choosing [text]
  (let [create-atom (fn [idx] (reagent/atom (contains? (@text-data :idxs-to-highlight) idx)))
        split-text (map-indexed
                    (fn [idx itm] [idx itm (create-atom idx)])
                    (re-seq #"[А-Яа-я\w]+|\W" text))]
    [:div [:p "Выберите слова для выделения:"]
     (when text (doall (for [[i t h-atom] split-text]
                         ^{:key i} [word-to-choose t h-atom])))
     [change-screen-panel #(swap! text-data assoc :idxs-to-highlight
                                  (set (for [[i _ h-atom] split-text :when (true? @h-atom)] i)))]]))

(defn template-textarea [title text idxs-to-highlight find-extra-params]
  (let [template
        (corpuscula.template-generator/generate-template
         title (re-seq #"[А-Яа-я\w]+|\W" text) idxs-to-highlight (find-extra-params))]
    [:div [:textarea
           {:value template
            :read-only true
            :rows 7}]
     [:input.footer {:type "button" :value "Скопировать шаблон"
                     :on-click #(write-text-into-clipboard template)}]]))

(defn checkbox [value title]
  [:div
   [:input
    {:type "checkbox"
     :checked @value
     :on-change #(swap! value not)}]
   [:label.pointer {:on-click #(swap! value not)} title]])

(defn template-output [title text idxs-to-highlight]
  (let [keys '(:rnc? :add-params? :without-quotes? :publisher-without-quotes?)
        captions '("источник – НКРЯ" "использовать названия параметров" "заголовок без кавычек" "издание без кавычек")
        a-values (map #(reagent/atom %) (conj (repeat false) true))
        checkboxes (map vector keys captions a-values)]
    [:div
     [:p "Шаблон:"]
     [template-textarea title text idxs-to-highlight #(zipmap keys (for [value a-values] @value))]
     (for [[key caption a-value] checkboxes] ^{:key key} [checkbox a-value caption])
     [change-screen-panel]]))

(defn screens []
  [[text-input-screen]
   [words-to-highlight-choosing (@text-data :text)]
   [template-output (@text-data :title) (@text-data :text) (@text-data :idxs-to-highlight)]])

(defn main []
  (fn []
    [:div.container
     (nth (screens) @current-screen)]))

(rd/render [main]
           (. js/document (getElementById "app")))