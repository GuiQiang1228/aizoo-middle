**根据logType的不同、job状态的不同，部分字段有所差异，大致格式不变

```
{
	"logType": "outLog",
	"outLog": { // 如果logType=errorLog，这里的key值也是errorLog
		"data": [{
			"line": 10,
			"id": 10,
			"text": "9:This is a log text...."
		}, {
			"line": 11,
			"id": 11,
			"text": "10:This is a log text...."
		}, {
			"line": 12,
			"id": 12,
			"text": "11:This is a log text...."
		}, {
			"line": 13,
			"id": 13,
			"text": "12:This is a log text...."
		}, {
			"line": 14,
			"id": 14,
			"text": "13:This is a log text...."
		}, {
			"line": 15,
			"id": 15,
			"text": "14:This is a log text...."
		}, {
			"line": 16,
			"id": 16,
			"text": "15:This is a log text...."
		}, {
			"line": 17,
			"id": 17,
			"text": "16:This is a log text...."
		}, {
			"line": 18,
			"id": 18,
			"text": "17:This is a log text...."
		}, {
			"line": 19,
			"id": 19,
			"text": "18:This is a log text...."
		}, {
			"line": 20,
			"id": 20,
			"text": "19:This is a log text...."
		}],
		"start": 10,
		"lineNum": 71, // 文件总行数
		"end": 20,
		"finished": true
	},
	"isLogFile": true
}
```