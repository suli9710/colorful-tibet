UPDATE scenic_spots
SET image_url = REPLACE(image_url, '.png', '.jpg')
WHERE image_url IN (
    '/images/spots/墨脱.png',
    '/images/spots/雍布拉康.png',
    '/images/spots/拉姆拉错.png',
    '/images/spots/扎达土林.png',
    '/images/spots/纳木那尼峰.png',
    '/images/spots/当惹雍错.png'
);
