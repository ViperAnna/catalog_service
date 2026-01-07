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
            status: 'inactive',
            createdAt: '2024-01-15T10:30:00Z',
            updatedAt: '2024-01-15T10:30:00Z'
        },
        {
            id: 2,
            name: 'Одежда',
            description: 'Мужская, женская и детская одежда',
            image: '/images/categories/clothes.jpg',
            status: 'active',
            createdAt: '2024-01-16T11:00:00Z',
            updatedAt: '2024-01-16T11:00:00Z'
        },
        {
            id: 3,
            name: 'Книги',
            description: 'Художественная литература, учебники, бизнес-книги',
            status: 'active',
            createdAt: '2024-01-17T09:15:00Z',
            updatedAt: '2024-01-17T09:15:00Z'
        },
        {
            id: 4,
            name: 'Спорт и отдых',
            description: 'Спортивный инвентарь, туризм, велосипеды',
            image: '/images/categories/sport.jpg',
            status: 'active',
            createdAt: '2024-01-18T14:20:00Z',
            updatedAt: '2024-01-18T14:20:00Z'
        },
        {
            id: 5,
            name: 'Красота и здоровье',
            description: 'Косметика, парфюмерия, средства ухода',
            image: '/images/categories/health.jpg',
            status: 'active',
            createdAt: '2024-01-19T12:00:00Z',
            updatedAt: '2024-01-19T12:00:00Z'
        },
        {
            id: 6,
            name: 'Бытовая техника',
            description: 'Холодильники, стиральные машины, пылесосы',
            image: '/images/categories/appliances.jpg',
            status: 'active',
            createdAt: '2024-01-20T09:45:00Z',
            updatedAt: '2024-01-20T09:45:00Z'
        },
        {
            id: 7,
            name: 'Мебель',
            description: 'Диваны, кровати, шкафы, столы и стулья',
            image: '/images/categories/furniture.jpg',
            status: 'active',
            createdAt: '2024-01-21T15:30:00Z',
            updatedAt: '2024-01-21T15:30:00Z'
        },
        {
            id: 8,
            name: 'Продукты питания',
            description: 'Напитки, консервы, молочные продукты',
            image: '/images/categories/food.jpg',
            status: 'active',
            createdAt: '2024-01-22T08:20:00Z',
            updatedAt: '2024-01-22T08:20:00Z'
        },
        {
            id: 9,
            name: 'Автотовары',
            description: 'Автозапчасти, шины, аксессуары',
            image: '/images/categories/car.jpg',
            status: 'active',
            createdAt: '2024-01-23T11:10:00Z',
            updatedAt: '2024-01-23T11:10:00Z'
        },
        {
            id: 10,
            name: 'Детские товары',
            description: 'Игрушки, коляски, детская мебель',
            image: '/images/categories/baby.jpg',
            status: 'pending',
            createdAt: '2024-01-24T14:45:00Z',
            updatedAt: '2024-01-24T14:45:00Z'
        },
        {
            id: 11,
            name: 'Ювелирные изделия',
            description: 'Кольца, серьги, браслеты, часы',
            image: '/images/categories/jewellery.jpg',
            status: 'active',
            createdAt: '2024-01-25T16:30:00Z',
            updatedAt: '2024-01-25T16:30:00Z'
        },
        {
            id: 12,
            name: 'Строительство и ремонт',
            description: 'Инструменты, материалы, сантехника',
            image: '/images/categories/construction.jpg',
            status: 'active',
            createdAt: '2024-01-26T10:15:00Z',
            updatedAt: '2024-01-26T10:15:00Z'
        },
        {
            id: 13,
            name: 'Канцелярия',
            description: 'Бумага, ручки, папки, офисные принадлежности',
            image: '/images/categories/office.jpg',
            status: 'active',
            createdAt: '2024-01-27T13:50:00Z',
            updatedAt: '2024-01-27T13:50:00Z'
        },
        {
            id: 14,
            name: 'Товары для животных',
            description: 'Корм, аксессуары, игрушки для питомцев',
            image: '/images/categories/pet.jpg',
            status: 'active',
            createdAt: '2024-01-28T09:25:00Z',
            updatedAt: '2024-01-28T09:25:00Z'
        },
        {
            id: 15,
            name: 'Музыкальные инструменты',
            description: 'Гитары, пианино, ударные, духовые',
            image: '/images/categories/music.jpg',
            status: 'pending',
            createdAt: '2024-01-29T11:40:00Z',
            updatedAt: '2024-01-29T11:40:00Z'
        },
        {
            id: 16,
            name: 'Садовый инвентарь',
            description: 'Газонокосилки, инструменты, семена',
            image: '/images/categories/garden.jpg',
            status: 'active',
            createdAt: '2024-01-30T14:10:00Z',
            updatedAt: '2024-01-30T14:10:00Z'
        },
        {
            id: 17,
            name: 'Цифровые товары',
            description: 'Программное обеспечение, игры, подписки',
            image: '/images/categories/digital.jpg',
            status: 'active',
            createdAt: '2024-01-31T16:20:00Z',
            updatedAt: '2024-01-31T16:20:00Z'
        },
        {
            id: 18,
            name: 'Антиквариат',
            description: 'Старинные вещи, коллекционные предметы',
            image: '/images/categories/antiques.jpg',
            status: 'inactive',
            createdAt: '2024-02-01T10:05:00Z',
            updatedAt: '2024-02-01T10:05:00Z'
        },
        {
            id: 19,
            name: 'Туризм и путешествия',
            description: 'Чемоданы, рюкзаки, палатки',
            image: '/images/categories/tourism.jpg',
            status: 'active',
            createdAt: '2024-02-02T12:30:00Z',
            updatedAt: '2024-02-02T12:30:00Z'
        },
        {
            id: 20,
            name: 'Хобби и творчество',
            description: 'Наборы для рукоделия, рисования, моделирования',
            image: '/images/categories/hobby.jpg',
            status: 'active',
            createdAt: '2024-02-03T15:15:00Z',
            updatedAt: '2024-02-03T15:15:00Z'
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