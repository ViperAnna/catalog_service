import React from 'react';
import {FiCheck, FiXCircle} from 'react-icons/fi';

const CategoryStatusEditor = ({
                                  isEditing,
                                  editedStatus,
                                  isSubmitting,
                                  onStatusChange
                              }) => {
    if (!isEditing) return null;

    return (
        <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
                Статус категории
            </label>
            <div className="flex space-x-4">
                <button
                    type="button"
                    onClick={() => onStatusChange('ACTIVE')}
                    disabled={isSubmitting}
                    className={`flex-1 flex items-center justify-center px-4 py-3 rounded-lg border-2 transition-all duration-200 font-medium disabled:opacity-50 disabled:cursor-not-allowed ${
                        editedStatus === 'ACTIVE'
                            ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                            : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                    }`}
                >
                    <FiCheck className="mr-2"/>
                    Активная
                </button>
                <button
                    type="button"
                    onClick={() => onStatusChange('INACTIVE')}
                    disabled={isSubmitting}
                    className={`flex-1 flex items-center justify-center px-4 py-3 rounded-lg border-2 transition-all duration-200 font-medium disabled:opacity-50 disabled:cursor-not-allowed ${
                        editedStatus === 'INACTIVE'
                            ? 'border-red-500 bg-red-50 text-red-700'
                            : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                    }`}
                >
                    <FiXCircle className="mr-2"/>
                    Неактивная
                </button>
            </div>
        </div>
    );
};

export default CategoryStatusEditor;