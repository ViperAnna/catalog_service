import React, {useEffect, useRef, useState} from 'react';
import {FiGrid, FiPlus} from 'react-icons/fi';
import CategoryList from './components/CategoryList/index.jsx';
import LoadingState from '../../components/LoadingState.jsx';
import EmptyState from './components/EmptyState.jsx';
import CategoriesCounter from './components/CategoriesCounter.jsx';
import CreateCategoryModal from "./components/CreateCategoryModal/index.jsx";
import {useStore} from "../../store/useStore.js";

const HomePage = () => {
    const {categories, loading, fetchCategories} = useStore();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [showAll, setShowAll] = useState(false);
    const scrollRef = useRef(null);

    useEffect(() => {
        fetchCategories();
    }, [fetchCategories]);

    const scrollLeft = () => {
        scrollRef.current?.scrollBy({left: -300, behavior: 'smooth'});
    };

    const scrollRight = () => {
        scrollRef.current?.scrollBy({left: 300, behavior: 'smooth'});
    };

    const toggleView = () => setShowAll(!showAll);

    if (loading) return <LoadingState/>;

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="text-center mb-12">
                <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-4">
                    Добро пожаловать в MiniMarket
                </h1>
                <p className="text-xl text-gray-600 max-w-2xl mx-auto">
                    Найдите товары по категориям
                </p>
            </div>
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900">
                    Популярные категории
                </h2>

                <div className="flex items-center space-x-4">
                    <button
                        onClick={toggleView}
                        className="flex items-center gap-2 px-4 py-2 bg-emerald-50 text-emerald-700 rounded-lg hover:bg-emerald-100 transition-colors"
                    >
                        <FiGrid className="w-4 h-4"/>
                        <span className="font-medium">
                            {showAll ? 'Свернуть' : 'Все категории'}
                        </span>
                    </button>
                    <button
                        onClick={() => setIsModalOpen(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
                    >
                        <FiPlus className="w-4 h-4"/>
                        Добавить категорию
                    </button>
                </div>
            </div>
            {categories.length === 0 ? (
                <EmptyState/>
            ) : (
                <CategoryList
                    categories={categories}
                    showAll={showAll}
                    scrollRef={scrollRef}
                    scrollLeft={scrollLeft}
                    scrollRight={scrollRight}
                />
            )}
            {!showAll && categories.length > 0 && <CategoriesCounter count={categories.length}/>}
            <CreateCategoryModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
            />
        </div>
    );
};

export default HomePage;