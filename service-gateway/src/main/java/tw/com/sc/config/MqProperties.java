package tw.com.sc.config;

// 這個檔案可以刪除，因為:
// 1. MqConfig 已經包含了所有 MQ 相關的配置
// 2. MqConfig 也有提供 MQConnectionFactory bean
// 3. 避免有兩個類都使用 @ConfigurationProperties(prefix = "mq")
