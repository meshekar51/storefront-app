-- Product IDs 1..8 match the stock rows seeded by inventory-service.
INSERT INTO products (sku, name, description, category, price) VALUES
  ('AUD-001', 'Studio Headphones',   'Closed-back, 40 mm drivers, 30 h battery.',         'Audio',   129.00),
  ('AUD-002', 'Pocket Speaker',      'Waterproof Bluetooth speaker with 12 h playback.',  'Audio',    49.90),
  ('HOM-001', 'Pour-Over Kettle',    'Gooseneck kettle with temperature control.',        'Kitchen',  79.00),
  ('HOM-002', 'Ceramic Mug Set',     'Four stoneware mugs, 350 ml, dishwasher safe.',     'Kitchen',  34.50),
  ('OUT-001', 'Trail Backpack 28L',  'Light daypack with rain cover and hip belt.',       'Outdoor',  89.00),
  ('OUT-002', 'Insulated Bottle',    'Keeps drinks cold for 24 h, 750 ml.',                'Outdoor',  24.95),
  ('DSK-001', 'Desk Lamp',           'Dimmable LED lamp with USB-C charging port.',        'Desk',     59.00),
  ('DSK-002', 'Notebook A5 (3-pack)','Dot-grid, 120 gsm paper, lay-flat binding.',          'Desk',     18.00);
