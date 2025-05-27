-- 创建饮水机表
CREATE TABLE IF NOT EXISTS water_dispenser (
    id INT PRIMARY KEY,
    locate VARCHAR(100) NOT NULL,
    tds VARCHAR(50) NOT NULL,
    status INT NOT NULL
);

-- 创建饮水记录表
CREATE TABLE IF NOT EXISTS drink_record (
    id INT AUTO_INCREMENT PRIMARY KEY,
    card_id VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    dispenser_id INT NOT NULL,
    time VARCHAR(20) NOT NULL,
    drink INT NOT NULL,
    FOREIGN KEY (dispenser_id) REFERENCES water_dispenser(id)
);