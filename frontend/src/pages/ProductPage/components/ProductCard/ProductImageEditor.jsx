import React, {useRef} from 'react';
import {FiUpload, FiX} from 'react-icons/fi';
import {toast} from 'react-hot-toast';

const MAX_SIZE = 5 * 1024 * 1024;

const ProductImageEditor = ({
                                keptImages,
                                newPreviews,
                                onRemoveKept,
                                onRemoveNew,
                                onAddFiles,
                                isSubmitting,
                                uploadProgress,
                            }) => {
    const fileInputRef = useRef(null);
    const total = keptImages.length + newPreviews.length;

    const handleAdd = (e) => {
        const files = Array.from(e.target.files);
        if (fileInputRef.current) fileInputRef.current.value = '';
        if (!files.length) return;

        const invalid = files.find(f => !f.type.startsWith('image/'));
        if (invalid) {
            toast.error('Допустимы только файлы изображений');
            return;
        }
        const oversized = files.find(f => f.size > MAX_SIZE);
        if (oversized) {
            toast.error('Размер каждого файла не должен превышать 5 МБ');
            return;
        }
        onAddFiles(files);
    };

    return (
        <div className="md:w-2/5 bg-white p-6 md:p-8 border-r border-gray-100">
            <label className="block text-sm font-medium text-gray-700 mb-2">
                Изображения <span className="text-red-500">*</span>
                <span className="text-gray-400 font-normal ml-2">(минимум 1, jpeg/jpg)</span>
            </label>

            {total > 0 && (
                <div className="grid grid-cols-3 gap-3 mb-3">
                    {keptImages.map((src, idx) => (
                        <div key={`kept-${idx}`} className="relative group">
                            <img
                                src={src}
                                alt={`image-${idx}`}
                                className="w-full h-24 object-cover rounded-lg border border-gray-200"
                            />
                            <button
                                type="button"
                                onClick={() => onRemoveKept(idx)}
                                disabled={isSubmitting}
                                className="absolute top-1 right-1 p-1 bg-white/90 rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow disabled:opacity-50"
                            >
                                <FiX className="w-3 h-3 text-gray-700"/>
                            </button>
                        </div>
                    ))}
                    {newPreviews.map((src, idx) => (
                        <div key={`new-${idx}`} className="relative group">
                            <img
                                src={src}
                                alt={`new-${idx}`}
                                className="w-full h-24 object-cover rounded-lg border-2 border-emerald-300"
                            />
                            <span className="absolute bottom-1 left-1 px-1.5 py-0.5 bg-emerald-500 text-white text-[10px] rounded">
                                новое
                            </span>
                            <button
                                type="button"
                                onClick={() => onRemoveNew(idx)}
                                disabled={isSubmitting}
                                className="absolute top-1 right-1 p-1 bg-white/90 rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow disabled:opacity-50"
                            >
                                <FiX className="w-3 h-3 text-gray-700"/>
                            </button>
                        </div>
                    ))}
                </div>
            )}

            {total === 0 && (
                <p className="text-xs text-red-500 mb-3">Добавьте хотя бы одно изображение</p>
            )}

            {isSubmitting && uploadProgress > 0 && uploadProgress < 100 && (
                <div className="mb-3">
                    <div className="text-xs text-gray-600 mb-1">Загрузка: {uploadProgress}%</div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                            className="bg-emerald-500 h-2 rounded-full transition-all duration-300"
                            style={{width: `${uploadProgress}%`}}
                        />
                    </div>
                </div>
            )}

            <div
                onClick={() => !isSubmitting && fileInputRef.current?.click()}
                className={`border-2 border-dashed border-gray-300 rounded-lg p-6 text-center transition-all ${
                    isSubmitting ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:border-emerald-500 hover:bg-emerald-50/50'
                }`}
            >
                <FiUpload className="w-8 h-8 text-gray-400 mx-auto mb-2"/>
                <p className="text-gray-600 font-medium text-sm">Нажмите для добавления фото</p>
                <p className="text-xs text-gray-400 mt-1">JPG/JPEG до 5 МБ каждый</p>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/jpg"
                    multiple
                    onChange={handleAdd}
                    disabled={isSubmitting}
                    className="hidden"
                />
            </div>
        </div>
    );
};

export default ProductImageEditor;
