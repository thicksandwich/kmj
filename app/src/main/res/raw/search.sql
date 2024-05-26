SELECT 
    kanji, 
    kana, 
    english
FROM 
    jmdict_fts
WHERE 
    jmdict_fts MATCH ?
ORDER BY 
    -- Prioritise verbs (starting with "to")
    CASE 
        WHEN english LIKE 'to %' THEN 0
        ELSE 1
    END,
	-- Within the "english" column, if the search_word is close to the start of the definition, prioritise it
    CASE 
        WHEN instr(english, ?) > 0 THEN -instr(english, ?)
        ELSE -1000 
    END desc,
	-- Take into account the priority score, calculated using kanji_common and kana_column
    priority_score
