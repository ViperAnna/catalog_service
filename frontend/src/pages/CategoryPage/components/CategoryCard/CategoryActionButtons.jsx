import React from 'react';
import {FiEdit2, FiSave, FiTrash2, FiX} from 'react-icons/fi';

const CategoryActionButtons = ({
                                   isEditing,
                                   hasChanges,
                                   isValid,
                                   isSubmitting,
                                   onEditClick,
                                   onSaveClick,
                                   onCancelClick,
                                   onDeleteClick,
                                   showDeleteButton = true,
                                   isDeleting = false
                               }) => {
    return (
        <div className="flex justify-end mb-6 space-x-3">
            {isEditing ? (
                <div className="flex space-x-3">
                    <button
                        type="button"
                        onClick={onCancelClick}
                        disabled={isSubmitting}
                        className="flex items-center px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors duration-200 font-medium shadow-sm border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <FiX className="mr-2"/>
                        Отмена
                    </button>
                    <button
                        type="button"
                        onClick={onSaveClick}
                        disabled={!hasChanges || isSubmitting || !isValid}
                        className={`flex items-center px-4 py-2 rounded-lg transition-colors duration-200 font-medium shadow-sm disabled:opacity-50 disabled:cursor-not-allowed ${
                            hasChanges && !isSubmitting && isValid
                                ? 'bg-emerald-500 text-white hover:bg-emerald-600'
                                : 'bg-gray-300 text-gray-500'
                        }`}
                    >
                        {isSubmitting ? (
                            <>
                                <div
                                    className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"
                                />
                                Сохранение...
                            </>
                        ) : (
                            <>
                                <FiSave className="mr-2"/>
                                Сохранить
                            </>
                        )}
                    </button>
                </div>
            ) : (
                <>
                    {showDeleteButton && onDeleteClick && (
                        <button
                            type="button"
                            onClick={onDeleteClick}
                            disabled={isDeleting}
                            className="flex items-center px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors duration-200 font-medium shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                            title="Удалить категорию"
                        >
                            <FiTrash2 className="mr-2"/>
                            Удалить
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={onEditClick}
                        className="flex items-center px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors duration-200 font-medium shadow-sm"
                        title="Редактировать категорию"
                    >
                        <FiEdit2 className="mr-2"/>
                        Редактировать
                    </button>
                </>
            )}
        </div>
    );
};

export default CategoryActionButtons;