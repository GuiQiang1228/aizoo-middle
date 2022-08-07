## 方法作用

组织某个job整体的资源使用情况（不分pid）

## jobkey存在时（任务未结束）

```
{
	"259-10.102.33.49": {
		"JobID": " 259",
		"Cluster": " cluster",
		"UserGroup": " aizoo-slurm/aizoo-slurm",
		"State": " RUNNING",
		"Nodes": " 1",
		"Corespernode": " 2",
		"CPUUtilized": " 00",
		"CPUEfficiency": " 0.00% of 1-02",
		"JobWallclocktime": " 13",
		"MemoryUtilized": " 0.00 MB (estimated maximum)",
		"MemoryEfficiency": " 0.00% of 2.00 GB (2.00 GB/node)",
		"WARNING": " Efficiency statistics may be misleading for RUNNING jobs."
	}
}
```

## jobkey不存在时（任务已结束）
```
{
	"114-10.102.33.49": {
		"JobID": " 114",
		"Cluster": " cluster",
		"UserGroup": " aizoo-slurm/aizoo-slurm",
		"State": " COMPLETED (exit code 0)",
		"Nodes": " 1",
		"Corespernode": " 2",
		"CPUUtilized": " 00",
		"CPUEfficiency": " 0.00% of 03",
		"JobWallclocktime": " 01",
		"MemoryUtilized": " 0.00 MB (estimated maximum)",
		"MemoryEfficiency": " 0.00% of 2.00 GB (2.00 GB/node)"
	}
}
```