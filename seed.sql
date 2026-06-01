
-- 颜色 (44色)
INSERT IGNORE INTO color (name, hex_code, category, is_active) VALUES
('法拉利红','#FF0000','亮光',1),('烈焰橙','#FF4500','亮光',1),('鎏金黄','#FFD700','金属',1),
('荧光黄','#FFFF00','亮光',1),('苹果绿','#32CD32','亮光',1),('翠绿','#006400','金属',1),
('蒂芙尼蓝','#81D8D0','哑光',1),('天蓝','#1E90FF','亮光',1),('宝蓝','#0000CD','金属',1),
('深海蓝','#000080','亮光',1),('星空紫','#8B00FF','金属',1),('玫瑰金','#FF69B4','金属',1),
('樱花粉','#FFB6C1','哑光',1),('冰莓粉','#FF80AB','哑光',1),('巧克力棕','#8B4513','亮光',1),
('碳纤维黑','#1C1C1C','哑光',1),('钢琴黑','#000000','亮光',1),('陶瓷白','#FAFAFA','亮光',1),
('珍珠白','#FFF5EE','金属',1),('磨砂白','#F5F5F5','哑光',1),('水泥灰','#A9A9A9','哑光',1),
('战斗灰','#696969','哑光',1),('纳多灰','#808080','金属',1),('火山灰','#708090','金属',1),
('液态银','#C0C0C0','金属',1),('电镀银','#BEBEBE','金属',1),('香槟金','#DAA520','金属',1),
('午夜紫','#4B0082','金属',1),('薰衣草紫','#9932CC','哑光',1),('橄榄绿','#556B2F','金属',1),
('军绿色','#2E8B57','哑光',1),('荧光绿','#00FF00','亮光',1),('马卡龙蓝','#87CEEB','哑光',1),
('马卡龙粉','#FFC0CB','哑光',1),('马卡龙绿','#98FF98','哑光',1),('极光紫','#9400D3','金属',1),
('极光蓝','#00CED1','金属',1),('珊瑚橙','#FF7F50','亮光',1),('暗夜绿','#228B22','亮光',1),
('高光红','#DC143C','亮光',1),('高光蓝','#4169E1','亮光',1),('红黑渐变','#8B0000','渐变',1),
('蓝紫渐变','#6A5ACD','渐变',1),('金粉渐变','#C71585','渐变',1);

-- 改色膜品牌 (8个)
INSERT IGNORE INTO brand (name, website) VALUES
('3M','https://www.3m.com.cn'),('艾利丹尼森','https://www.averydennison.cn'),
('XPEL','https://www.xpel.com.cn'),('圣科','https://www.suntek.com'),
('龙膜','https://www.llumar.com.cn'),('威固','https://www.v-kool.cn'),
('固驰',''),('卡莱斯','');

-- 门店 (5家已审核)
INSERT IGNORE INTO shop (id,name,address,lat,lng,phone,description,status) VALUES
(1,'极致改色工坊','杭州市西湖区文三路508号',30.2741,120.1301,'0571-88001234','3M/XPEL授权施工中心，5年质保','APPROVED'),
(2,'车颜悦色旗舰店','杭州市滨江区江南大道228号',30.2085,120.2111,'0571-87005678','500平米无尘车间，进口设备','APPROVED'),
(3,'膜法世家汽车贴膜','杭州市余杭区文一西路1500号',30.2820,120.0150,'0571-86009876','10年老店一站式服务','APPROVED'),
(4,'星光改色贴膜','杭州市拱墅区莫干山路100号',30.2950,120.1480,'0571-88004567','个性定制改色/彩绘/拉花','APPROVED'),
(5,'酷玩车身改色','杭州市上城区河坊街200号',30.2420,120.1680,'0571-89006543','文艺复古风格改色工作室','APPROVED');

-- 案例 (8个施工案例)
INSERT IGNORE INTO shop_case (id,shop_id,before_url,after_url,color_id,car_model,description,likes) VALUES
(1,1,'/cases/before1.jpg','/cases/after1.jpg',1,'奔驰 C260L','白改法拉利红，施工3天',128),
(2,1,'/cases/before2.jpg','/cases/after2.jpg',17,'宝马 3系','黑改钢琴黑',89),
(3,2,'/cases/before3.jpg','/cases/after3.jpg',8,'保时捷 911','银改蒂芙尼蓝',256),
(4,2,'/cases/before1.jpg','/cases/after4.jpg',11,'奥迪 A6L','黑改星空紫',67),
(5,3,'/cases/before2.jpg','/cases/after5.jpg',3,'特斯拉 Model Y','白改鎏金黄',312),
(6,4,'/cases/before3.jpg','/cases/after6.jpg',14,'理想 L9','银改冰莓粉',198),
(7,4,'/cases/before1.jpg','/cases/after7.jpg',23,'路虎 揽胜','黑改纳多灰',145),
(8,5,'/cases/before4.jpg','/cases/after8.jpg',6,'蔚来 ET5','白改翠绿',73);

-- 热门车型属性
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='奔驰' AND model_name='C Class';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='奔驰' AND model_name='GLC';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='宝马' AND model_name='3 Series';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='宝马' AND model_name='X5';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='奥迪' AND model_name='A4';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='奥迪' AND model_name='Q5';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='保时捷' AND model_name='Cayenne';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='保时捷' AND model_name='Panamera';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='特斯拉' AND model_name='Model 3';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='特斯拉' AND model_name='Model Y';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='理想' AND model_name='L9';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='蔚来' AND model_name='ET5';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='比亚迪' AND model_name='宋 Pro';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='比亚迪' AND model_name='汉';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='路虎' AND model_name='Range Rover';
UPDATE car_model SET body_type='SUV', year='2024' WHERE brand_name='沃尔沃' AND model_name='XC60';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='雷克萨斯' AND model_name='ES';
UPDATE car_model SET body_type='轿车', year='2024' WHERE brand_name='凯迪拉克' AND model_name='CT5';
