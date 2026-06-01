import React, {useEffect, useRef, useState} from 'react';
import {FiPlus, FiUpload, FiX} from 'react-icons/fi';
import {toast} from 'react-hot-toast';
import {useStore} from '../../../../store/useStore.js';
import {PRODUCT_STATUS, getProductStatusLabel} from '../../../../utils/productStatus.js';

const INITIAL_FORM = {
    name: '',
    description: '',
    brand: '',
    price: '',
    status: PRODUCT_STATUS.ACTIVE,
};

const ProductForm = ({onClose}) => {
    const {createProduct, fetchProducts, categories, fetchCategories} = useStore();

    const [form, setForm] = useState(INITIAL_FORM);
    const [imageFiles, setImageFiles] = useState([]);
    const [imagePreviews, setImagePreviews] = useState([]);
    const [selectedCategories, setSelectedCategories] = useState([]);
    const [characteristics, setCharacteristics] = useState([{key: '', value: ''}]);
    const [tags, setTags] = useState([]);
    const [tagInput, setTagInput] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (categories.length === 0) fetchCategories();
    }, []);

    const isValid =
        form.name.trim() &&
        form.description.trim() &&
        form.brand.trim() &&
        form.price !== '' &&
        Number(form.price) > 0 &&
        imageFiles.length > 0 &&
        selectedCategories.length > 0 &&
        tags.length > 0;

    const handleChange = (e) => {
        const {name, value} = e.target;
        setForm(prev => ({...prev, [name]: value}));
    };

    const handleImageAdd = (e) => {
        const files = Array.from(e.target.files);
        if (!files.length) return;

        const invalid = files.find(f => !f.type.startsWith('image/'));
        if (invalid) {
            toast.error('Допустимы только файлы изображений');
            return;
        }
        const oversized = files.find(f => f.size > 5 * 1024 * 1024);
        if (oversized) {
            toast.error('Размер каждого файла не должен превышать 5 МБ');
            return;
        }

        setImageFiles(prev => [...prev, ...files]);

        files.forEach(file => {
            const reader = new FileReader();
            reader.onloadend = () => setImagePreviews(prev => [...prev, reader.result]);
            reader.readAsDataURL(file);
        });

        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    const handleImageRemove = (idx) => {
        setImageFiles(prev => prev.filter((_, i) => i !== idx));
        setImagePreviews(prev => prev.filter((_, i) => i !== idx));
    };

    const handleCategoryToggle = (id) => {
        setSelectedCategories(prev =>
            prev.includes(id) ? prev.filter(c => c !== id) : [...prev, id]
        );
    };

    const handleCharacteristicChange = (idx, field, value) => {
        setCharacteristics(prev => {
            const next = [...prev];
            next[idx] = {...next[idx], [field]: value};
            return next;
        });
    };

    const addCharacteristic = () => {
        setCharacteristics(prev => [...prev, {key: '', value: ''}]);
    };

    const removeCharacteristic = (idx) => {
        setCharacteristics(prev => prev.filter((_, i) => i !== idx));
    };

    const handleTagKeyDown = (e) => {
        if ((e.key === 'Enter' || e.key === ',') && tagInput.trim()) {
            e.preventDefault();
            const newTag = tagInput.trim().replace(/,$/, '');
            if (newTag && !tags.includes(newTag)) {
                setTags(prev => [...prev, newTag]);
            }
            setTagInput('');
        }
    };

    const removeTag = (idx) => setTags(prev => prev.filter((_, i) => i !== idx));

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!isValid) return;

        setIsSubmitting(true);
        setUploadProgress(0);
        try {
            const fd = new FormData();
            fd.append('name', form.name.trim());
            fd.append('description', form.description.trim());
            fd.append('brand', form.brand.trim());
            fd.append('price', form.price);
            fd.append('status', form.status);

            imageFiles.forEach(file => fd.append('images', file));
            selectedCategories.forEach(id => fd.append('categories', id));
            tags.forEach(tag => fd.append('tag', tag));

            const validChars = characteristics.filter(c => c.key.trim() && c.value.trim());
            validChars.forEach(({key, value}) => {
                fd.append(`characteristic.attributes[${key.trim()}]`, value.trim());
            });
            if (validChars.length === 0) {
                fd.append('characteristic.attributes[—]', '—');
            }

            const response = await createProduct(fd, {
                onUploadProgress: (ev) => {
                    setUploadProgress(Math.round((ev.loaded * 100) / ev.total));
                }
            });

            if (response?.status === 201) {
                toast.success('Товар успешно создан!');
                await fetchProducts();
                onClose();
            } else {
                toast.error(response?.data?.message || 'Не удалось создать товар');
            }
        } catch (error) {
            if (error?.message?.includes('already exists')) {
                toast.error('Товар с таким артикулом уже существует');
            } else {
                toast.error(error?.message || 'Не удалось создать товар');
            }
        } finally {
            setIsSubmitting(false);
            setUploadProgress(0);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
            <div className="space-y-6">

                {/* Основные поля */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Название <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            name="name"
                            value={form.name}
                            onChange={handleChange}
                            placeholder="Например: Камера Sony Alpha"
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Бренд <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            name="brand"
                            value={form.brand}
                            onChange={handleChange}
                            placeholder="Например: Sony"
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Цена (₽) <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="number"
                            name="price"
                            value={form.price}
                            onChange={handleChange}
                            placeholder="0"
                            min="0"
                            step="0.01"
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                            required
                        />
                    </div>

                    <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Описание <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            name="description"
                            value={form.description}
                            onChange={handleChange}
                            placeholder="Краткое описание товара"
                            rows="3"
                            maxLength={500}
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all resize-none"
                            required
                        />
                        <p className="text-xs text-gray-400 mt-1 text-right">{form.description.length}/500</p>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Статус
                        </label>
                        <select
                            name="status"
                            value={form.status}
                            onChange={handleChange}
                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all bg-white"
                        >
                            {Object.values(PRODUCT_STATUS).map(s => (
                                <option key={s} value={s}>{getProductStatusLabel(s)}</option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* Изображения */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Изображения <span className="text-red-500">*</span>
                        <span className="text-gray-400 font-normal ml-2">(минимум 1, jpeg/jpg)</span>
                    </label>

                    {imagePreviews.length > 0 && (
                        <div className="grid grid-cols-3 gap-3 mb-3">
                            {imagePreviews.map((src, idx) => (
                                <div key={idx} className="relative group">
                                    <img
                                        src={src}
                                        alt={`preview-${idx}`}
                                        className="w-full h-24 object-cover rounded-lg border border-gray-200"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => handleImageRemove(idx)}
                                        className="absolute top-1 right-1 p-1 bg-white/90 rounded-full opacity-0 group-hover:opacity-100 transition-opacity shadow"
                                    >
                                        <FiX className="w-3 h-3 text-gray-700"/>
                                    </button>
                                </div>
                            ))}
                        </div>
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
                        onClick={() => fileInputRef.current?.click()}
                        className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center cursor-pointer hover:border-emerald-500 hover:bg-emerald-50/50 transition-all"
                    >
                        <FiUpload className="w-8 h-8 text-gray-400 mx-auto mb-2"/>
                        <p className="text-gray-600 font-medium text-sm">Нажмите для добавления фото</p>
                        <p className="text-xs text-gray-400 mt-1">JPG/JPEG до 5 МБ каждый</p>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/jpeg,image/jpg"
                            multiple
                            onChange={handleImageAdd}
                            className="hidden"
                        />
                    </div>
                </div>

                {/* Категории */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Категории <span className="text-red-500">*</span>
                    </label>
                    {categories.length === 0 ? (
                        <p className="text-sm text-gray-400">Категории не найдены</p>
                    ) : (
                        <div className="grid grid-cols-2 gap-2 max-h-40 overflow-y-auto border border-gray-200 rounded-lg p-3">
                            {categories.map(cat => (
                                <label key={cat.id} className="flex items-center gap-2 cursor-pointer group">
                                    <input
                                        type="checkbox"
                                        checked={selectedCategories.includes(cat.id)}
                                        onChange={() => handleCategoryToggle(cat.id)}
                                        className="w-4 h-4 text-emerald-600 focus:ring-emerald-500 rounded"
                                    />
                                    <span className="text-sm text-gray-700 group-hover:text-emerald-700 transition-colors line-clamp-1">
                                        {cat.name}
                                    </span>
                                </label>
                            ))}
                        </div>
                    )}
                </div>

                {/* Теги */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Теги <span className="text-red-500">*</span>
                        <span className="text-gray-400 font-normal ml-2">(Enter или запятая для добавления)</span>
                    </label>
                    <div className="border border-gray-300 rounded-lg p-3 focus-within:ring-2 focus-within:ring-emerald-500 focus-within:border-emerald-500 transition-all">
                        <div className="flex flex-wrap gap-2 mb-2">
                            {tags.map((tag, idx) => (
                                <span key={idx}
                                      className="inline-flex items-center gap-1 px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-sm font-medium">
                                    {tag}
                                    <button type="button" onClick={() => removeTag(idx)}>
                                        <FiX className="w-3 h-3"/>
                                    </button>
                                </span>
                            ))}
                        </div>
                        <input
                            type="text"
                            value={tagInput}
                            onChange={e => setTagInput(e.target.value)}
                            onKeyDown={handleTagKeyDown}
                            placeholder="Введите тег..."
                            className="w-full outline-none text-sm text-gray-700 placeholder-gray-400"
                        />
                    </div>
                </div>

                {/* Характеристики */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Характеристики
                    </label>
                    <div className="space-y-2">
                        {characteristics.map((char, idx) => (
                            <div key={idx} className="flex gap-2 items-center">
                                <input
                                    type="text"
                                    value={char.key}
                                    onChange={e => handleCharacteristicChange(idx, 'key', e.target.value)}
                                    placeholder="Параметр"
                                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none"
                                />
                                <input
                                    type="text"
                                    value={char.value}
                                    onChange={e => handleCharacteristicChange(idx, 'value', e.target.value)}
                                    placeholder="Значение"
                                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none"
                                />
                                <button
                                    type="button"
                                    onClick={() => removeCharacteristic(idx)}
                                    disabled={characteristics.length === 1}
                                    className="p-2 text-gray-400 hover:text-red-500 disabled:opacity-30 transition-colors"
                                >
                                    <FiX className="w-4 h-4"/>
                                </button>
                            </div>
                        ))}
                        <button
                            type="button"
                            onClick={addCharacteristic}
                            className="flex items-center gap-2 text-sm text-emerald-600 hover:text-emerald-800 font-medium"
                        >
                            <FiPlus className="w-4 h-4"/>
                            Добавить характеристику
                        </button>
                    </div>
                </div>
            </div>

            <div className="flex justify-end gap-4 mt-8 pt-6 border-t border-gray-100">
                <button
                    type="button"
                    onClick={onClose}
                    disabled={isSubmitting}
                    className="px-6 py-3 text-gray-700 font-medium hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
                >
                    Отмена
                </button>
                <button
                    type="submit"
                    disabled={!isValid || isSubmitting}
                    className={`px-6 py-3 font-medium rounded-lg transition-all flex items-center gap-2 ${
                        !isValid || isSubmitting
                            ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                            : 'bg-emerald-600 text-white hover:bg-emerald-700'
                    }`}
                >
                    {isSubmitting ? (
                        <>
                            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"/>
                            Создание...
                        </>
                    ) : (
                        <>
                            <FiPlus className="w-5 h-5"/>
                            Создать товар
                        </>
                    )}
                </button>
            </div>
        </form>
    );
};

export default ProductForm;
