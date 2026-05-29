import React, {useEffect} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {FiArrowLeft} from 'react-icons/fi';
import {useStore} from '../../store/useStore.js';
import LoadingState from '../../components/LoadingState.jsx';
import ProductBreadcrumbs from './components/ProductBreadcrumbs.jsx';
import ProductNotFound from './components/ProductNotFound.jsx';
import ProductCard from './components/ProductCard/index.jsx';

const ProductPage = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const {currentProduct, loading, error, fetchProductById} = useStore();

    useEffect(() => {
        if (id) fetchProductById(id);
    }, [id]);

    const handleSuccess = () => {
        if (id) fetchProductById(id);
    };

    if (loading) return <LoadingState/>;
    if (error || !currentProduct) return <ProductNotFound/>;

    return (
        <div className="container mx-auto px-4 py-8">
            <ProductBreadcrumbs productName={currentProduct.name}/>
            <div className="mb-8">
                <button
                    onClick={() => navigate(-1)}
                    className="inline-flex items-center text-emerald-600 hover:text-emerald-800 font-medium"
                >
                    <FiArrowLeft className="mr-2"/>
                    Назад к товарам
                </button>
            </div>
            <ProductCard product={currentProduct} onSuccess={handleSuccess}/>
        </div>
    );
};

export default ProductPage;
