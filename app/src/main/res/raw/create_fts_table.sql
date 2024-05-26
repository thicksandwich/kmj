-- Create a virtual table for effective searching
CREATE VIRTUAL TABLE jmdict_fts USING fts4(
    kanji,
    kana,
    english,
	priority_score
);

-- Copy necessary values into the virtual table and assign a priority score for ranking
-- Filter: definition applies to a specific kanji
INSERT INTO jmdict_fts (kanji, kana, english, priority_score)
SELECT
    kanji,
    kana,
    english,
    (kanji_pri + kana_pri) / 2 AS priority_score
FROM (
    SELECT
        kanji.value AS kanji,
        GROUP_CONCAT(DISTINCT kana.value) AS kana,
        GROUP_CONCAT(DISTINCT definition.value) AS english,
        MIN(
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'ichi1') > 0 THEN 1 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'news1') > 0 THEN 2 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'spec1') > 0 THEN 3 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'gai1') > 0 THEN 4 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'ichi2') > 0 THEN 5 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'news2') > 0 THEN 6 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'spec2') > 0 THEN 7 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'gai2') > 0 THEN 8 ELSE 9 END
        ) AS kanji_pri,
		MIN(
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'ichi1') > 0 THEN 1 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'news1') > 0 THEN 2 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'spec1') > 0 THEN 3 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'gai1') > 0 THEN 4 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'ichi2') > 0 THEN 5 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'news2') > 0 THEN 6 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'spec2') > 0 THEN 7 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'gai2') > 0 THEN 8 ELSE 9 END
        ) AS kana_pri
    FROM
        entry
    INNER JOIN
        kanji ON entry.id = kanji.entry_id
    LEFT JOIN
        kanji_common ON kanji.id = kanji_common.kanji_id
    LEFT JOIN
        kana ON entry.id = kana.entry_id
	LEFT JOIN
        kana_common ON kana.id = kana_common.kana_id
    INNER JOIN
        sense ON entry.id = sense.entry_id
    INNER JOIN
        definition ON definition.sense_id = sense.id
	INNER JOIN
	    sense_applies_to_kanji ON sense_applies_to_kanji.sense_id = sense.id
	WHERE
	    kanji.value = sense_applies_to_kanji.value
    GROUP BY
        kanji
);

-- Copy necessary values into the virtual table and assign a priority score for ranking
-- Filter: ignore records in which the kanji already exists
INSERT INTO jmdict_fts (kanji, kana, english, priority_score)
SELECT
    kanji,
    kana,
    english,
    (kanji_pri + kana_pri) / 2 AS priority_score
FROM (
    SELECT
        kanji.value AS kanji,
        GROUP_CONCAT(DISTINCT kana.value) AS kana,
        GROUP_CONCAT(DISTINCT definition.value) AS english,
        MIN(
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'ichi1') > 0 THEN 1 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'news1') > 0 THEN 2 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'spec1') > 0 THEN 3 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'gai1') > 0 THEN 4 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'ichi2') > 0 THEN 5 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'news2') > 0 THEN 6 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'spec2') > 0 THEN 7 ELSE 9 END,
            CASE WHEN instr(REPLACE(kanji_common.value, ' ', ''), 'gai2') > 0 THEN 8 ELSE 9 END
        ) AS kanji_pri,
        MIN(
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'ichi1') > 0 THEN 1 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'news1') > 0 THEN 2 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'spec1') > 0 THEN 3 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'gai1') > 0 THEN 4 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'ichi2') > 0 THEN 5 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'news2') > 0 THEN 6 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'spec2') > 0 THEN 7 ELSE 9 END,
            CASE WHEN instr(REPLACE(kana_common.value, ' ', ''), 'gai2') > 0 THEN 8 ELSE 9 END
        ) AS kana_pri
    FROM
        entry
    INNER JOIN
        kanji ON entry.id = kanji.entry_id
    LEFT JOIN
        kanji_common ON kanji.id = kanji_common.kanji_id
    LEFT JOIN
        kana ON entry.id = kana.entry_id
    LEFT JOIN
        kana_common ON kana.id = kana_common.kana_id
    INNER JOIN
        sense ON entry.id = sense.entry_id
    INNER JOIN
        definition ON definition.sense_id = sense.id
    LEFT JOIN
        sense_applies_to_kanji ON sense_applies_to_kanji.sense_id = sense.id
    WHERE
        kanji.value NOT IN (SELECT kanji FROM jmdict_fts)
    GROUP BY
        kanji
);

