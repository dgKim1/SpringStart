-- products: stock 제거, price 타입 변경, name 길이 조정, status 및 CHECK 추가
ALTER TABLE products
    DROP COLUMN stock;

ALTER TABLE products
    ALTER COLUMN name TYPE VARCHAR(100),
    ALTER COLUMN price TYPE INT USING price::INT,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'FOR_SALE',
    ADD CONSTRAINT chk_product_price CHECK (price >= 0);

-- product_options 신규 생성
CREATE TABLE product_options
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id       BIGINT       NOT NULL,
    name             VARCHAR(100) NOT NULL,
    additional_price INT          NOT NULL DEFAULT 0,
    stock            INT          NOT NULL DEFAULT 0,
    CONSTRAINT chk_additional_price CHECK (additional_price >= 0),
    CONSTRAINT chk_option_stock CHECK (stock >= 0)
);
