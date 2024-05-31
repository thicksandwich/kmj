CREATE INDEX idx_kana_value ON kana(value);
CREATE INDEX idx_entry_id ON entry(id);
CREATE INDEX idx_kanji_entry_id ON kanji(entry_id);
CREATE INDEX idx_kana_entry_id ON kana(entry_id);
CREATE INDEX idx_kana_common_kana_id ON kana_common(kana_id);
CREATE INDEX idx_kanji_common_kanji_id ON kanji_common(kanji_id);

CREATE VIRTUAL TABLE jmdict_fts USING fts4(
    entry_id,
    kanji,
    kana,
    english,
    kanji_common,
	kana_common,
	priority
);

INSERT INTO jmdict_fts (entry_id, kanji, kana, english, kanji_common, kana_common, priority)
WITH prioritised_entries AS (
    SELECT
        entry.id AS entry_id,
        kanji.value AS kanji,
        kana.value AS kana,
        GROUP_CONCAT(DISTINCT definition.value) AS english,
        GROUP_CONCAT(DISTINCT kanji_common.value) AS kanji_common,
		GROUP_CONCAT(DISTINCT kana_common.value) AS kana_common,
        CASE
			WHEN instr(kanji_common.value, 'nf01') > 0 THEN 1
			WHEN instr(kanji_common.value, 'nf02') > 0 THEN 2
			WHEN instr(kanji_common.value, 'nf03') > 0 THEN 3
			WHEN instr(kanji_common.value, 'nf04') > 0 THEN 4
			WHEN instr(kanji_common.value, 'nf05') > 0 THEN 5
			WHEN instr(kanji_common.value, 'nf06') > 0 THEN 6
			WHEN instr(kanji_common.value, 'nf07') > 0 THEN 7
			WHEN instr(kanji_common.value, 'nf08') > 0 THEN 8
			WHEN instr(kanji_common.value, 'nf09') > 0 THEN 9
			WHEN instr(kanji_common.value, 'nf10') > 0 THEN 10
			WHEN instr(kanji_common.value, 'nf11') > 0 THEN 11
			WHEN instr(kanji_common.value, 'nf12') > 0 THEN 12
			WHEN instr(kanji_common.value, 'nf13') > 0 THEN 13
			WHEN instr(kanji_common.value, 'nf14') > 0 THEN 14
			WHEN instr(kanji_common.value, 'nf15') > 0 THEN 15
			WHEN instr(kanji_common.value, 'nf16') > 0 THEN 16
			WHEN instr(kanji_common.value, 'nf17') > 0 THEN 17
			WHEN instr(kanji_common.value, 'nf18') > 0 THEN 18
			WHEN instr(kanji_common.value, 'nf19') > 0 THEN 19
			WHEN instr(kanji_common.value, 'nf20') > 0 THEN 20
			WHEN instr(kanji_common.value, 'nf21') > 0 THEN 21
			WHEN instr(kanji_common.value, 'nf22') > 0 THEN 22
			WHEN instr(kanji_common.value, 'nf23') > 0 THEN 23
			WHEN instr(kanji_common.value, 'nf24') > 0 THEN 24
			WHEN instr(kanji_common.value, 'nf25') > 0 THEN 25
			WHEN instr(kanji_common.value, 'nf26') > 0 THEN 26
			WHEN instr(kanji_common.value, 'nf27') > 0 THEN 27
			WHEN instr(kanji_common.value, 'nf28') > 0 THEN 28
			WHEN instr(kanji_common.value, 'nf29') > 0 THEN 29
			WHEN instr(kanji_common.value, 'nf30') > 0 THEN 30
			WHEN instr(kanji_common.value, 'nf31') > 0 THEN 31
			WHEN instr(kanji_common.value, 'nf32') > 0 THEN 32
			WHEN instr(kanji_common.value, 'nf33') > 0 THEN 33
			WHEN instr(kanji_common.value, 'nf34') > 0 THEN 34
			WHEN instr(kanji_common.value, 'nf35') > 0 THEN 35
			WHEN instr(kanji_common.value, 'nf36') > 0 THEN 36
			WHEN instr(kanji_common.value, 'nf37') > 0 THEN 37
			WHEN instr(kanji_common.value, 'nf38') > 0 THEN 38
			WHEN instr(kanji_common.value, 'nf39') > 0 THEN 39
			WHEN instr(kanji_common.value, 'nf40') > 0 THEN 40
			WHEN instr(kanji_common.value, 'nf41') > 0 THEN 41
			WHEN instr(kanji_common.value, 'nf42') > 0 THEN 42
			WHEN instr(kanji_common.value, 'nf43') > 0 THEN 43
			WHEN instr(kanji_common.value, 'nf44') > 0 THEN 44
			WHEN instr(kanji_common.value, 'nf45') > 0 THEN 45
			WHEN instr(kanji_common.value, 'nf46') > 0 THEN 46
			WHEN instr(kanji_common.value, 'nf47') > 0 THEN 47
			WHEN instr(kanji_common.value, 'nf48') > 0 THEN 48
			WHEN instr(kanji_common.value, 'nf49') > 0 THEN 49
			WHEN instr(kanji_common.value, 'nf50') > 0 THEN 50
			ELSE 51
		END +
	    CASE 
            WHEN instr(kanji_common.value, 'ichi1') > 0 THEN 0 
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'ichi2') > 0 THEN 25 
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'news1') > 0 THEN 0
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'news2') > 0 THEN 25 
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'spec1') > 0 THEN 0 
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'spec2') > 0 THEN 25 
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'gai1') > 0 THEN 0 
            ELSE 51
		END +
		CASE 
            WHEN instr(kanji_common.value, 'gai2') > 0 THEN 25 
            ELSE 51
		END +
		 CASE
			WHEN instr(kana_common.value, 'nf01') > 0 THEN 1
			WHEN instr(kana_common.value, 'nf02') > 0 THEN 2
			WHEN instr(kana_common.value, 'nf03') > 0 THEN 3
			WHEN instr(kana_common.value, 'nf04') > 0 THEN 4
			WHEN instr(kana_common.value, 'nf05') > 0 THEN 5
			WHEN instr(kana_common.value, 'nf06') > 0 THEN 6
			WHEN instr(kana_common.value, 'nf07') > 0 THEN 7
			WHEN instr(kana_common.value, 'nf08') > 0 THEN 8
			WHEN instr(kana_common.value, 'nf09') > 0 THEN 9
			WHEN instr(kana_common.value, 'nf10') > 0 THEN 10
			WHEN instr(kana_common.value, 'nf11') > 0 THEN 11
			WHEN instr(kana_common.value, 'nf12') > 0 THEN 12
			WHEN instr(kana_common.value, 'nf13') > 0 THEN 13
			WHEN instr(kana_common.value, 'nf14') > 0 THEN 14
			WHEN instr(kana_common.value, 'nf15') > 0 THEN 15
			WHEN instr(kana_common.value, 'nf16') > 0 THEN 16
			WHEN instr(kana_common.value, 'nf17') > 0 THEN 17
			WHEN instr(kana_common.value, 'nf18') > 0 THEN 18
			WHEN instr(kana_common.value, 'nf19') > 0 THEN 19
			WHEN instr(kana_common.value, 'nf20') > 0 THEN 20
			WHEN instr(kana_common.value, 'nf21') > 0 THEN 21
			WHEN instr(kana_common.value, 'nf22') > 0 THEN 22
			WHEN instr(kana_common.value, 'nf23') > 0 THEN 23
			WHEN instr(kana_common.value, 'nf24') > 0 THEN 24
			WHEN instr(kana_common.value, 'nf25') > 0 THEN 25
			WHEN instr(kana_common.value, 'nf26') > 0 THEN 26
			WHEN instr(kana_common.value, 'nf27') > 0 THEN 27
			WHEN instr(kana_common.value, 'nf28') > 0 THEN 28
			WHEN instr(kana_common.value, 'nf29') > 0 THEN 29
			WHEN instr(kana_common.value, 'nf30') > 0 THEN 30
			WHEN instr(kana_common.value, 'nf31') > 0 THEN 31
			WHEN instr(kana_common.value, 'nf32') > 0 THEN 32
			WHEN instr(kana_common.value, 'nf33') > 0 THEN 33
			WHEN instr(kana_common.value, 'nf34') > 0 THEN 34
			WHEN instr(kana_common.value, 'nf35') > 0 THEN 35
			WHEN instr(kana_common.value, 'nf36') > 0 THEN 36
			WHEN instr(kana_common.value, 'nf37') > 0 THEN 37
			WHEN instr(kana_common.value, 'nf38') > 0 THEN 38
			WHEN instr(kana_common.value, 'nf39') > 0 THEN 39
			WHEN instr(kana_common.value, 'nf40') > 0 THEN 40
			WHEN instr(kana_common.value, 'nf41') > 0 THEN 41
			WHEN instr(kana_common.value, 'nf42') > 0 THEN 42
			WHEN instr(kana_common.value, 'nf43') > 0 THEN 43
			WHEN instr(kana_common.value, 'nf44') > 0 THEN 44
			WHEN instr(kana_common.value, 'nf45') > 0 THEN 45
			WHEN instr(kana_common.value, 'nf46') > 0 THEN 46
			WHEN instr(kana_common.value, 'nf47') > 0 THEN 47
			WHEN instr(kana_common.value, 'nf48') > 0 THEN 48
			WHEN instr(kana_common.value, 'nf49') > 0 THEN 49
			WHEN instr(kana_common.value, 'nf50') > 0 THEN 50
			ELSE 51
		END +
	    CASE 
            WHEN instr(kana_common.value, 'ichi1') > 0 THEN 0 
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'ichi2') > 0 THEN 25 
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'news1') > 0 THEN 0
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'news2') > 0 THEN 25 
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'spec1') > 0 THEN 0 
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'spec2') > 0 THEN 25 
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'gai1') > 0 THEN 0 
            ELSE 51
		END +
		CASE 
            WHEN instr(kana_common.value, 'gai2') > 0 THEN 25 
            ELSE 51
		END		
		AS priority
    FROM
        entry
    LEFT JOIN kanji ON entry.id = kanji.entry_id
    LEFT JOIN kana ON entry.id = kana.entry_id
    LEFT JOIN sense ON entry.id = sense.entry_id
    LEFT JOIN definition ON definition.sense_id = sense.id
    LEFT JOIN kanji_common ON kanji.id = kanji_common.kanji_id
    LEFT JOIN kana_common ON kana.id = kana_common.kana_id
    GROUP BY entry.id, kanji.value, kana.value
)
SELECT
    entry_id,
    kanji,
    kana,
    english,
    kanji_common,
	kana_common,
	priority
FROM
    prioritised_entries
ORDER BY priority;
