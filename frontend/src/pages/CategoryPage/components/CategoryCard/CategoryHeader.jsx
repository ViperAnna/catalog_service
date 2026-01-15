import React from 'react';
import {FiCheck, FiPackage, FiXCircle} from 'react-icons/fi';

const CategoryHeader = ({
                            isEditing,
                            editedName,
                            editedStatus,
                            isSubmitting,
                            onNameChange,
                            onKeyDown
                        }) => {
    const getStatusStyles = (status) => {
        if (status === 'ACTIVE') {
            return {
                bg: 'bg-emerald-100',
                text: 'text-emerald-800',
                icon: <FiCheck className="mr-1.5"/>,
                label: 'Активная'
            };
        } else {
            return {
                bg: 'bg-red-100',
                text: 'text-red-800',
                icon: <FiXCircle className="mr-1.5"/>,
                label: 'Неактивная'
            };
        }
    };

    const statusStyles = getStatusStyles(editedStatus);

    return (
        <div className="mb-6">
            <div className="mb-3 h-14">
                {isEditing ? (
                    <input
                        type="text"
                        value={editedName}
                        onChange={onNameChange}
                        onKeyDown={(e) => onKeyDown(e, 'name')}
                        className="text-3xl font-bold text-gray-900 w-full h-full px-3 py-2 border-2 border-emerald-500 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-300 focus:border-emerald-500"
                        autoFocus
                        placeholder="Введите название категории"
                        disabled={isSubmitting}
                    />
                ) : (
                    <h1 className="text-3xl font-bold text-gray-900 h-full flex items-center">
                        {editedName}
                    </h1>
                )}
            </div>

            <div className="flex flex-wrap gap-2">
                <div
                    className={`inline-flex items-center px-3 py-1 rounded-full ${statusStyles.bg} ${statusStyles.text} text-sm font-medium`}>
                    {statusStyles.icon}
                    {statusStyles.label}
                </div>

                <div
                    className="inline-flex items-center px-4 py-1 rounded-full bg-emerald-100 text-emerald-800 text-sm font-medium">
                    <FiPackage className="mr-2"/>
                    Категория товаров
                </div>
            </div>
        </div>
    );
};

export default CategoryHeader;