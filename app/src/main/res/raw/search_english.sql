SELECT 
    entry_id,
	kanji,
	group_concat(DISTINCT kana) as kana,
	english,
	priority
FROM 
    jmdict_fts
WHERE 
    english match ?
GROUP by 
    entry_id
ORDER BY
	 priority;
