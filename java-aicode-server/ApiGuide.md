10. 使用示例
    Postman调用方式：
    GET请求：http://localhost:8080/api/database/config
    获取当前配置信息
    POST请求：http://localhost:8080/api/database/export?outputPath=D:/backup/
    导出配置文件中指定的表到指定路径
    POST请求：http://localhost:8080/api/database/export-custom?outputPath=D:/backup/&tables=parking_book,parking_record
    导出指定的表到指定路径