import React from 'react';

const CategoryDescriptionEditor = ({
                                       isEditing,
                                       editedDescription,
                                       isSubmitting,
                                       onDescriptionChange,
                                       onKeyDown
                                   }) => {
    return (
        <div className="mb-8">
            {isEditing ? (
                <div className="h-32">
                    <textarea
                        id="description-input"
                        value={editedDescription}
                        onChange={onDescriptionChange}
                        onKeyDown={(e) => onKeyDown(e, 'description')}
                        className="text-gray-700 text-lg leading-relaxed w-full h-full px-3 py-2 border-2 border-emerald-500 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-300 focus:border-emerald-500 resize-none"
                        placeholder="Введите описание категории"
                        disabled={isSubmitting}
                    />
                </div>
            ) : (
                <div className="min-h-32">
                    <p className="text-gray-700 text-lg leading-relaxed">
                        {editedDescription || <span className="text-gray-400 italic">Нет описания</span>}
                    </p>
                </div>
            )}
        </div>
    );
};

export default CategoryDescriptionEditor;