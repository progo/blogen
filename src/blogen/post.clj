(ns blogen.post
  (:require
   [dire.core :refer [with-handler!]]
   [clj-time.format]
   [net.cgrand.enlive-html :as html]))

(defn title
  "Get the title of the post."
  [post]
  (-> (html/select post [:#content :.title])
      first
      html/text))

(defn tags
  "Get the tags of the post. Returns a seq."
  [post]
  (->> (html/select post [:h2 :.tag :span])
       (map :content)
       (map first)))

(defn created-date
  "Collect the CLOSED timestamp of the post as the creation date.
  Return a DateTime object."
  [post]
  (->> (html/select post [:span.timestamp-wrapper])
       (filter (fn [tag]
                 (-> (html/select tag [:span.timestamp-kwd])
                     first
                     :content
                     first
                     (= "CLOSED:"))))
       first
       ((fn [x] (html/select x [:span.timestamp])))
       first
       :content
       first
       (clj-time.format/parse
        (clj-time.format/formatter "[yyyy-MM-dd EEE HH:mm]"))
       ))

(with-handler! #'created-date
  "Skip NPE, indicating there was no date there."
  java.lang.NullPointerException
  (constantly nil))

(defn contents-raw
  "Get the raw (unprocessed) contents of the post"
  [post]
  (html/select post [:#content]))

(defn trim-well
  "Trim the usual whitespace and NBSP's."
  [s]
  (-> s
      (.replace "\u00a0" " ")  ; nbsp
      .trim))

(defn htmlify
  "Turn any enlive structure to (X)HTML string. Do note that this is
  not needed with enlive template functions."
  [parsed-stuff]
  (apply str (html/emit* parsed-stuff)))

(defn- remove-empty-paragraphs
  "Remove given enlive map if only containing whitespace. Return nil
  when removed. Checks for images and embedded."
  [par]
  (when (or (seq (trim-well (html/text par)))
            (seq (html/select par [#{:img :embed}])))
    par))

;; TODO: write walker to walk through tagsoup 'h' and do the NBSP
;; replace in every string found.
(defn clean-headings
  "Clear nbsp's from headings."
  [h]
  (assoc-in h [:content] (trim-well (html/text h))))

(defn contents-clean
  "Get processed contents of the post."
  [post]
  (let [contents (contents-raw post)]
    (-> contents
        ; duplicate headline off
        (html/at [:h1.title] nil)
        ; tags off
        (html/at [:span.tag] nil)
        ; closed TS off. The empty :p is left.
        (html/at [:p :span.timestamp-wrapper] nil)
        ; empty lines off
        (html/at [:p :> :br] nil)
        ; dvipng
        (html/transform [[:img (html/attr-starts :src "ltxpng/")]]
                        (html/set-attr :class "dvipng"))
        ; empty paragraphs off
        (html/transform [:p] remove-empty-paragraphs)
        ; trim headings
        ; Let's disable for time being. Let's be more clever about it.
        ; (html/at [#{:h1 :h2 :h3 :h4 :h5}] clean-headings)
        )))

;; The UID/persistent ID tries to keep as simple as possible.
(defn persistent-id
  "A unique string ID that should be as persistent as possible, stay
  the same in case of one single post entry no matter what."
  [post]
  {:post [(seq %)]}
  (clj-time.format/unparse
   (clj-time.format/formatter "yyyyMMdd_HHmm")
   (created-date post)))

(defn count-words
  "Collect a number of words in the post."
  [post]
  (let [txt (first (html/texts post))]
    (count (re-seq #"\s+" txt))))

(defn all-info
  "Given parsed post map, collect all details in a map to return."
  [post]
  (let [contents-cleaned (contents-clean post)]
    {:uid (persistent-id post)
     :title (title post)
     :tags (tags post)
     :created (created-date post)
     :content contents-cleaned
     :word-count (count-words contents-cleaned)
     :original-content post}))

(defn ready-to-publish?
  "Is given post a ready one, ready to go in the internets? The
  argument 'post' is a parsed map of a post."
  [post]
  (not (or
        (nil? (:created post))
        (some #{"no-export" "wait"}
              (:tags post)))))
