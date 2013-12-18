(ns blogen.post
  (:require [net.cgrand.enlive-html :as html]))

(defn title
  "Get the title of the post."
  [post]
  (-> (html/select post [:#content :.title])
      first
      :content
      first))

(defn tags
  "Get the tags of the post. Returns a seq."
  [post]
  (->> (html/select post [:h2 :.tag :span])
       (map :content)
       (map first)))

;; We may consider closedness as the original publication time.
(defn closed-time
  ""
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
       first :content first))

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

;; TODO: write zipper to walk through tagsoup 'h' and do the NBSP
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

  
(defn complete-data
  [data]
  (for [post-data data]
    (let [post (:parsed post-data)]
      (assoc post-data :title (title post)))))
