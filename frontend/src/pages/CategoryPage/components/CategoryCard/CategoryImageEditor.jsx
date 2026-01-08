import React, {useRef} from 'react';
import {FiImage, FiUpload, FiX} from 'react-icons/fi';
import EmptyImage from "../../../../components/EmptyImage.jsx";
import {toast} from 'react-hot-toast';

const CategoryImageEditor = ({
                                 isEditing,
                                 editedName,
                                 imagePreview,
                                 originalImage,
                                 onImageUpload,
                                 onRemoveImage,
                                 onRestoreImage
                             }) => {
    const fileInputRef = useRef(null);

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        if (!file.type.startsWith('image/')) {
            toast.error('Пожалуйста, выберите файл изображения');
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            toast.error('Размер файла не должен превышать 5MB');
            return;
        }

        onImageUpload(file);
    };

    return (
        <div className="md:w-2/5 bg-white p-6 md:p-8 border-r border-gray-100">
            <div className="h-[400px] md:h-full flex items-center justify-center bg-gray-50 rounded-xl relative">
                {isEditing ? (
                    <>
                        <div className="w-full h-full flex items-center justify-center">
                            {imagePreview ? (
                                <div className="relative w-full h-full flex items-center justify-center">
                                    <img
                                        src={imagePreview}
                                        alt={editedName}
                                        className="max-w-full max-h-full object-contain p-4"
                                        style={{maxHeight: '280px'}}
                                    />
                                    <button
                                        type="button"
                                        onClick={onRemoveImage}
                                        className="absolute top-3 right-3 p-2 bg-white/90 backdrop-blur-sm rounded-full hover:bg-white transition-colors shadow-lg"
                                    >
                                        <FiX className="w-5 h-5 text-gray-700"/>
                                    </button>
                                </div>
                            ) : (
                                <div
                                    onClick={() => fileInputRef.current?.click()}
                                    className="w-full h-full border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-emerald-500 hover:bg-emerald-50/50 transition-all flex flex-col items-center justify-center"
                                >
                                    <FiUpload className="w-12 h-12 text-gray-400 mx-auto mb-4"/>
                                    <p className="text-gray-600 font-medium">
                                        Нажмите для загрузки изображения
                                    </p>
                                    <p className="text-sm text-gray-500 mt-2">
                                        PNG, JPG или WebP до 5MB
                                    </p>
                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        accept="image/*"
                                        onChange={handleFileSelect}
                                        className="hidden"
                                    />
                                </div>
                            )}
                        </div>

                        {originalImage && !imagePreview && (
                            <div className="absolute bottom-4 left-0 right-0 text-center">
                                <button
                                    onClick={onRestoreImage}
                                    className="inline-flex items-center px-4 py-2 text-sm bg-white/90 backdrop-blur-sm text-gray-700 hover:text-gray-900 hover:bg-white rounded-lg transition-colors shadow-sm border border-gray-200"
                                >
                                    <FiImage className="mr-2"/>
                                    Восстановить оригинальное изображение
                                </button>
                            </div>
                        )}
                    </>
                ) : (
                    <div className="w-full h-full flex items-center justify-center">
                        {imagePreview ? (
                            <img
                                src={imagePreview}
                                alt={editedName}
                                className="max-w-full max-h-full object-contain p-4"
                                style={{maxHeight: '280px'}}
                            />
                        ) : (
                            <EmptyImage size="xl"/>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default CategoryImageEditor;