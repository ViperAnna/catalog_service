import React from 'react';
import {Link} from 'react-router-dom';
import {FiArrowLeft} from 'react-icons/fi';

const BackButton = () => {
    return (
        <div className="mb-8">
            <Link
                to="/"
                className="inline-flex items-center text-emerald-600 hover:text-emerald-800"
            >
                <FiArrowLeft className="mr-2"/>
                Назад к категориям
            </Link>
        </div>
    );
};

export default BackButton;