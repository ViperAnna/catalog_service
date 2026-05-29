import React, {useCallback, useEffect, useRef, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {FiPlus, FiSearch, FiX} from 'react-icons/fi';
import {useStore} from '../../store/useStore.js';
import LoadingState from '../../components/LoadingState.jsx';
import ProductGrid from './components/ProductGrid.jsx';
import EmptyState from './components/EmptyState.jsx';
import Pagination from './components/Pagination.jsx';
import CreateProductModal from './components/CreateProductModal/index.jsx';

const PAGE_SIZE = 8;

const ProductsPage = () => {
    const {products, pagination, loading, fetchProducts, fetchProductsByName, categories, fetchCategories} = useStore();
    const [searchParams, setSearchParams] = useSearchParams();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [isSearchMode, setIsSearchMode] = useState(false);
    const categoryId = searchParams.get('categoryId') || '';
    const page = Number(searchParams.get('page') || 0);
    const debounceRef = useRef(null);

    useEffect(() => {
        if (categories.length === 0) fetchCategories();
    }, []);

    useEffect(() => {
        if (!isSearchMode) {
            fetchProducts({page, size: PAGE_SIZE, categoryId: categoryId || undefined});
        }
    }, [page, categoryId, isSearchMode]);

    const handleSearchChange = (e) => {
        const value = e.target.value;
        setSearch(value);

        clearTimeout(debounceRef.current);
        if (value.trim().length >= 2) {
            debounceRef.current = setTimeout(() => {
                setIsSearchMode(true);
                fetchProductsByName(value.trim());
            }, 500);
        } else {
            setIsSearchMode(false);
        }
    };

    const clearSearch = () => {
        setSearch('');
        setIsSearchMode(false);
        fetchProducts({page: 0, size: PAGE_SIZE, categoryId: categoryId || undefined});
    };

    const handleCategoryChange = (e) => {
        const val = e.target.value;
        const next = new URLSearchParams(searchParams);
        if (val) {
            next.set('categoryId', val);
        } else {
            next.delete('categoryId');
        }
        next.delete('page');
        setSearchParams(next);
        clearSearch();
    };

    const handlePageChange = (newPage) => {
        const next = new URLSearchParams(searchParams);
        next.set('page', newPage);
        setSearchParams(next);
        window.scrollTo({top: 0, behavior: 'smooth'});
    };

    const selectedCategory = categories.find(c => c.id === categoryId);
    const hasFilters = !!categoryId || isSearchMode;

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="text-center mb-10">
                <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-4">
                    {selectedCategory ? selectedCategory.name : 'Все товары'}
                </h1>
                <p className="text-xl text-gray-600">
                    {selectedCategory
                        ? selectedCategory.description
                        : 'Найдите нужный товар в нашем каталоге'
                    }
                </p>
            </div>

            {/* Панель фильтров */}
            <div className="flex flex-col sm:flex-row gap-3 mb-8">
                {/* Поиск */}
                <div className="relative flex-1">
                    <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-5 h-5"/>
                    <input
                        type="text"
                        value={search}
                        onChange={handleSearchChange}
                        placeholder="Поиск по названию..."
                        className="w-full pl-10 pr-10 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                    />
                    {search && (
                        <button
                            onClick={clearSearch}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                        >
                            <FiX className="w-4 h-4"/>
                        </button>
                    )}
                </div>

                {/* Фильтр по категории */}
                <select
                    value={categoryId}
                    onChange={handleCategoryChange}
                    className="px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none bg-white text-gray-700 min-w-[200px]"
                >
                    <option value="">Все категории</option>
                    {categories.map(cat => (
                        <option key={cat.id} value={cat.id}>{cat.name}</option>
                    ))}
                </select>

                {/* Кнопка добавления */}
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium whitespace-nowrap"
                >
                    <FiPlus className="w-4 h-4"/>
                    Добавить товар
                </button>
            </div>

            {/* Контент */}
            {loading ? (
                <LoadingState/>
            ) : products.length === 0 ? (
                <EmptyState hasFilters={hasFilters}/>
            ) : (
                <>
                    {!isSearchMode && (
                        <p className="text-sm text-gray-500 mb-4">
                            Найдено товаров: {pagination.totalElements}
                        </p>
                    )}
                    {isSearchMode && (
                        <p className="text-sm text-gray-500 mb-4">
                            По запросу «{search}» найдено: {products.length}
                        </p>
                    )}
                    <ProductGrid products={products}/>
                    {!isSearchMode && (
                        <Pagination
                            page={pagination.page}
                            totalPages={pagination.totalPages}
                            totalElements={pagination.totalElements}
                            size={pagination.size}
                            onPageChange={handlePageChange}
                        />
                    )}
                </>
            )}

            <CreateProductModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
            />
        </div>
    );
};

export default ProductsPage;
