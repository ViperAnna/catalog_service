import React from 'react';
import {Link} from 'react-router-dom';
import {FiArrowRight, FiCheckCircle} from 'react-icons/fi';
import EmptyImage from "../../../../components/EmptyImage.jsx";
import moment from "moment";

const CategoryCard = ({category}) => {

    const getStatusColor = (status) => {
        switch (status) {
            case 'ACTIVE':
                return 'bg-green-100 text-green-800';
            case 'INACTIVE':
                return 'bg-red-100 text-red-800';
            default:
                return 'bg-blue-100 text-blue-800';
        }
    };

    const getStatusText = (status) => {
        switch (status) {
            case 'ACTIVE':
                return 'Активна';
            case 'INACTIVE':
                return 'Неактивна';
            default:
                return status;
        }
    };

    return (
        <Link to={`/categories/${category.id}`}>
            <div
                className="group bg-white rounded-xl shadow-md hover:shadow-lg transition-all duration-300 overflow-hidden border border-gray-100 hover:border-green-200">
                <div className="relative h-48 overflow-hidden">
                    {category.pictureUrl ? (
                        <img
                            src={category.pictureUrl}
                            alt={category.name}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                    ) : (
                        <EmptyImage/>
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent"></div>

                    <div className="absolute top-3 right-3">
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(category.status)}`}>
              {getStatusText(category.status)}
            </span>
                    </div>
                </div>

                <div className="p-5">
                    <div className="flex justify-between items-start mb-3">
                        <h3
                            title={category.name}
                            className="text-xl font-bold text-gray-900 group-hover:text-emerald-600
               transition-colors line-clamp-2 leading-tight h-[3.4rem]"
                        >
                            {category.name}
                        </h3>
                        <FiArrowRight
                            className="w-5 h-5 text-gray-400 group-hover:text-emerald-600 group-hover:translate-x-1 transition-all"/>
                    </div>

                    <p
                        title={category.description}
                        className="text-gray-600 mb-4 line-clamp-2 h-[2.8rem] leading-snug"
                    >
                        {category.description}
                    </p>

                    <div
                        className="flex items-center justify-between text-sm text-gray-500 pt-4 border-t border-gray-100">
                        <div className="flex items-center">
                            <FiCheckCircle className="w-4 h-4 text-green-500 mr-1"/>
                            <span>Доступно</span>
                        </div>
                        <div>

                            <span className="text-gray-400">Товары</span>
                        </div>
                    </div>

                    <div className="mt-3 text-xs text-gray-400">
                        Изменено: {moment(category.updatedAt, 'YYYY-MM-DD HH:mm:ss').format('DD.MM.YYYY HH:mm:ss')}
                    </div>
                </div>

                <div
                    className="h-1 bg-gradient-to-r from-emerald-500 to-emerald-700 transform scale-x-0 group-hover:scale-x-100 transition-transform duration-300"></div>
            </div>
        </Link>
    );
};

export default CategoryCard;