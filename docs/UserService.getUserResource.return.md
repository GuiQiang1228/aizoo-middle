```java
/**
* @Description: 根据用户名获取用户的资源使用情况
* @param username: 用户名
* @return: java.util.Map<java.lang.String,java.lang.Object> 资源使用情况
* @throws: URISyntaxException
*/
```

~~~
//返回类型是一个map，每个map的key是资源描述，value是具体的资源详情/数值
{
	//以下信息从数据库中的level表中获取
    maxCPU=10                       //当前等级下最大允许获取的CPU数量
    maxGPU=8					    //当前等级下最大允许获取的GPU数量
    maxDisk=30720				    //当前等级下最大允许占用的磁盘大小
    maxTotalApp=30                  //当前等级下最大可创建的Application数量
    maxRunningApp=10				//当前等级下最大允许运行中的Application数量
    maxTotalExperiment=30			//当前等级下最大可创建的Experiment数量
    maxRunningExperiment=10         //当前等级下最大允许运行中的Experiment数量
    maxMemory=10240                 //当前等级下最大允许运行内存
    maxTotalService=30				//当前等级下最大可创建的Service数量
    maxRunningService=10			//当前等级下最大允许运行中的Service数量
    
    //以下信息从数据库中的resource_usage表获取
    appliedCPU=8					//当前用户已经占用的CPU数量
    appliedGPU=0					//当前用户已经占用的GPU数量
    appliedMemory=9981				//当前用户已经占用的内存大小
    appliedDisk=219952				//当前用户已经占用的磁盘大小
    runningExperiment=8				//当前用户正在运行的Experiment数量
    runningService=8				//当前用户正在运行的Service数量
    runningApplication=8			//当前用户正在运行的Application数量
    
    //以下信息在对应的job表里获取
    existExperiment=15				//当前用户已创建的Experiment数量
    existApplication=15				//当前用户已创建的Application数量
    existService=15					//当前用户已创建的Service数量
}
~~~