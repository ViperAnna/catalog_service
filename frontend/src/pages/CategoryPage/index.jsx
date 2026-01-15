import React, {useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {useStore} from '../../store/useStore';
import LoadingState from '../../components/LoadingState';
import CategoryNotFound from './components/CategoryNotFound';
import CategoryBreadcrumbs from './components/CategoryBreadcrumbs';
import BackButton from './components/BackButton';
import CategoryCard from './components/CategoryCard/index.jsx';

const CategoryPage = () => {
    const {id} = useParams();
    const {
        currentCategory,
        loading,
        error,
        fetchCategoryById
    } = useStore();

    useEffect(() => {
        if (id) {
            fetchCategoryById(id);
        }
    }, [id]);

    const handleCategoryUpdate = () => {
        if (id) {
            fetchCategoryById(id);
        }
    };

    if (loading) return <LoadingState/>;
    if (error || !currentCategory) return <CategoryNotFound/>;

    return (
        <div className="container mx-auto px-4 py-8">
            <CategoryBreadcrumbs categoryName={currentCategory.name}/>
            <BackButton/>
            <CategoryCard
                category={currentCategory}
                onSuccess={handleCategoryUpdate}
            />

        </div>
    );
};

export default CategoryPage;