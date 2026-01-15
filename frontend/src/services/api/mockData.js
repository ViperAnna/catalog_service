export const loadFromLocalStorage = () => {
    const saved = localStorage.getItem('mock_store_data');
    if (saved) {
        const parsed = JSON.parse(saved);
        mockData.categories = parsed.categories || [];
    }
    return mockData.categories
};

export const saveToLocalStorage = () => {
    localStorage.setItem('mock_store_data', JSON.stringify(mockData));
};

export const mockData = {
    categories: [
        {
            id: 1,
            name: 'Электроника',
            description: 'Смартфоны, ноутбуки, планшеты и гаджеты',
            image: '/images/categories/electronics.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-15 10:30:00',
            updatedAt: '2024-01-20'
        },
        {
            id: 2,
            name: 'Одежда',
            description: 'Мужская, женская и детская одежда',
            image: '/images/categories/clothes.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-16 11:00:00',
            updatedAt: '2024-01-16 11:00:00'
        },
        {
            id: 3,
            name: 'Книги',
            description: 'Художественная литература, учебники, бизнес-книги',
            status: 'ACTIVE',
            createdAt: '2024-01-17 09:15:00',
            updatedAt: '2024-01-17 09:15:00'
        },
        {
            id: 4,
            name: 'Спорт и отдых',
            description: 'Спортивный инвентарь, туризм, велосипеды',
            image: '/images/categories/sport.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-18 14:20:00',
            updatedAt: '2024-01-18 14:20:00'
        },
        {
            id: 5,
            name: 'Красота и здоровье',
            description: 'Косметика, парфюмерия, средства ухода',
            image: '/images/categories/health.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-19 12:00:00',
            updatedAt: '2024-01-19 12:00:00'
        },
        {
            id: 6,
            name: 'Бытовая техника',
            description: 'Холодильники, стиральные машины, пылесосы',
            image: '/images/categories/appliances.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-20 09:45:00',
            updatedAt: '2024-01-20 09:45:00'
        },
        {
            id: 7,
            name: 'Мебель',
            description: 'Диваны, кровати, шкафы, столы и стулья',
            image: '/images/categories/furniture.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-21 15:30:00',
            updatedAt: '2024-01-21 15:30:00'
        },
        {
            id: 8,
            name: 'Продукты питания',
            description: 'Напитки, консервы, молочные продукты',
            image: '/images/categories/food.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-22 08:20:00',
            updatedAt: '2024-01-22 08:20:00'
        },
        {
            id: 9,
            name: 'Автотовары',
            description: 'Автозапчасти, шины, аксессуары',
            image: '/images/categories/car.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-23 11:10:00',
            updatedAt: '2024-01-23 11:10:00'
        },
        {
            id: 10,
            name: 'Детские товары',
            description: 'Игрушки, коляски, детская мебель',
            image: '/images/categories/baby.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-24 14:45:00',
            updatedAt: '2024-01-24 14:45:00'
        },
        {
            id: 11,
            name: 'Ювелирные изделия',
            description: 'Кольца, серьги, браслеты, часы',
            image: '/images/categories/jewellery.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-25 16:30:00',
            updatedAt: '2024-01-25 16:30:00'
        },
        {
            id: 12,
            name: 'Строительство и ремонт',
            description: 'Инструменты, материалы, сантехника',
            image: '/images/categories/construction.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-26 10:15:00',
            updatedAt: '2024-01-26 10:15:00'
        },
        {
            id: 13,
            name: 'Канцелярия',
            description: 'Бумага, ручки, папки, офисные принадлежности',
            image: '/images/categories/office.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-27 13:50:00',
            updatedAt: '2024-01-27 13:50:00'
        },
        {
            id: 14,
            name: 'Товары для животных',
            description: 'Корм, аксессуары, игрушки для питомцев',
            image: '/images/categories/pet.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-28 09:25:00',
            updatedAt: '2024-01-28 09:25:00'
        },
        {
            id: 15,
            name: 'Музыкальные инструменты',
            description: 'Гитары, пианино, ударные, духовые',
            image: '/images/categories/music.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-29 11:40:00',
            updatedAt: '2024-01-29 11:40:00'
        },
        {
            id: 16,
            name: 'Садовый инвентарь',
            description: 'Газонокосилки, инструменты, семена',
            image: '/images/categories/garden.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-30 14:10:00',
            updatedAt: '2024-01-30 14:10:00'
        },
        {
            id: 17,
            name: 'Цифровые товары',
            description: 'Программное обеспечение, игры, подписки',
            image: '/images/categories/digital.jpg',
            status: 'ACTIVE',
            createdAt: '2024-01-31 16:20:00',
            updatedAt: '2024-01-31 16:20:00'
        },
        {
            id: 18,
            name: 'Антиквариат',
            description: 'Старинные вещи, коллекционные предметы',
            image: '/images/categories/antiques.jpg',
            status: 'INACTIVE',
            createdAt: '2024-02-01 10:05:00',
            updatedAt: '2024-02-01 10:05:00'
        },
        {
            id: 19,
            name: 'Туризм и путешествия',
            description: 'Чемоданы, рюкзаки, палатки',
            image: '/images/categories/tourism.jpg',
            status: 'ACTIVE',
            createdAt: '2024-02-02 12:30:00',
            updatedAt: '2024-02-02 12:30:00'
        },
        {
            id: 20,
            name: 'Хобби и творчество',
            description: 'Наборы для рукоделия, рисования, моделирования',
            image: '/images/categories/hobby.jpg',
            status: 'ACTIVE',
            createdAt: '2024-02-03 15:15:00',
            updatedAt: '2024-02-03 15:15:00'
        }
    ],

    products: [
        {
            id: 1,
            name: 'Смартфон Samsung Galaxy S23',
            description: 'Флагманский смартфон с лучшей камерой',
            price: 79999,
            categoryId: 1,
            status: 'in_stock'
        },
        {
            id: 2,
            name: 'Ноутбук Apple MacBook Pro 16"',
            description: 'Мощный ноутбук для профессионалов',
            price: 249999,
            categoryId: 1,
            status: 'in_stock'
        },
        {
            id: 3,
            name: 'Футболка мужская хлопковая',
            description: 'Классическая футболка из 100% хлопка',
            price: 1499,
            categoryId: 2,
            status: 'in_stock'
        }
    ]
};