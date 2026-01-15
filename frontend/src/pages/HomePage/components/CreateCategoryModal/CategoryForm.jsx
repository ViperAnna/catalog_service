import React, {useEffect, useRef, useState} from 'react';
import {FiPlus, FiUpload, FiX} from 'react-icons/fi';
import {toast} from 'react-hot-toast';
import {useStore} from "../../../../store/useStore.js";

const CategoryForm = ({onClose}) => {
    const {createCategory, fetchCategories} = useStore();

    const [formData, setFormData] = useState({
        name: '',
        description: '',
        image: null,
        status: 'ACTIVE',
    });

    const [imagePreview, setImagePreview] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isFormValid, setIsFormValid] = useState(false);
    const fileInputRef = useRef(null);

    useEffect(() => {
        const isValid =
            formData.name.trim() !== '' &&
            formData.description.trim() !== '' &&
            formData.image !== null;
        setIsFormValid(isValid);
    }, [formData]);

    const handleInputChange = (e) => {
        const {name, value} = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleImageUpload = (e) => {
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

        setFormData(prev => ({
            ...prev,
            image: file
        }));

        const reader = new FileReader();
        reader.onloadend = () => {
            setImagePreview(reader.result);
        };
        reader.readAsDataURL(file);
    };

    const handleRemoveImage = () => {
        setFormData(prev => ({
            ...prev,
            image: null
        }));
        setImagePreview(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.name.trim() || !formData.description.trim()) {
            toast.error('Заполните обязательные поля');
            return;
        }

        setIsSubmitting(true);

        try {
            const formDataToSend = new FormData();
            formDataToSend.append('name', formData.name);
            formDataToSend.append('description', formData.description);
            formDataToSend.append('status', formData.status);
            if (formData.image) {
                formDataToSend.append('image', formData.image);
            }

            const response = await createCategory(formDataToSend);

            if (response?.status === 201) {
                toast.success('Категория успешно создана!');
                await fetchCategories();
                onClose();
            } else {
                toast.error(response.data.message);
            }
        } catch (error) {
            toast.error('Не удалось создать категорию. Попробуйте еще раз.');
        } finally {
            setIsSubmitting(false);
        }
    };

    const resetForm = () => {
        setFormData({
            name: '',
            description: '',
            image: null,
            status: 'ACTIVE',
        });
        setImagePreview(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

    return (
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
            <div className="space-y-6">

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Название категории
                        <span className="text-red-500 font-bold ml-1">*</span>
                    </label>
                    <input
                        type="text"
                        name="name"
                        value={formData.name}
                        onChange={handleInputChange}
                        placeholder="Например: Электроника, Одежда, Продукты"
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                        required
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Описание
                        <span className="text-red-500 font-bold ml-1">*</span>
                    </label>
                    <textarea
                        name="description"
                        value={formData.description}
                        onChange={handleInputChange}
                        placeholder="Краткое описание категории"
                        rows="3"
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all resize-none"
                        required
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Изображение категории
                        <span className="text-red-500 font-bold ml-1">*</span>
                    </label>

                    {imagePreview ? (
                        <div className="relative">
                            <img
                                src={imagePreview}
                                alt="Превью"
                                className="w-full h-48 object-cover rounded-lg"
                            />
                            <button
                                type="button"
                                onClick={handleRemoveImage}
                                className="absolute top-3 right-3 p-2 bg-white/90 backdrop-blur-sm rounded-full hover:bg-white transition-colors shadow-lg"
                            >
                                <FiX className="w-5 h-5 text-gray-700"/>
                            </button>
                        </div>
                    ) : (
                        <div
                            onClick={() => fileInputRef.current?.click()}
                            className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-emerald-500 hover:bg-emerald-50/50 transition-all"
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
                                onChange={handleImageUpload}
                                className="hidden"
                            />
                        </div>
                    )}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Статус категории
                    </label>
                    <div className="flex gap-4">
                        <label className="flex items-center">
                            <input
                                type="radio"
                                name="status"
                                value="ACTIVE"
                                checked={formData.status === 'ACTIVE'}
                                onChange={handleInputChange}
                                className="w-4 h-4 text-emerald-600 focus:ring-emerald-500"
                            />
                            <span className="ml-2 text-gray-700">Активная</span>
                        </label>
                        <label className="flex items-center">
                            <input
                                type="radio"
                                name="status"
                                value="INACTIVE"
                                checked={formData.status === 'INACTIVE'}
                                onChange={handleInputChange}
                                className="w-4 h-4 text-gray-600 focus:ring-gray-500"
                            />
                            <span className="ml-2 text-gray-700">Неактивная</span>
                        </label>
                    </div>
                </div>
            </div>

            <div className="flex justify-end gap-4 mt-8 pt-6 border-t border-gray-100">
                <button
                    type="button"
                    onClick={handleClose}
                    className="px-6 py-3 text-gray-700 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                    disabled={isSubmitting}
                >
                    Отмена
                </button>
                <button
                    type="submit"
                    disabled={!isFormValid || isSubmitting}
                    className={`px-6 py-3 font-medium rounded-lg transition-all flex items-center gap-2 ${
                        !isFormValid
                            ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                            : 'bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed'
                    }`}
                >
                    {isSubmitting ? (
                        <>
                            <div
                                className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                            Создание...
                        </>
                    ) : (
                        <>
                            <FiPlus className="w-5 h-5"/>
                            Создать категорию
                        </>
                    )}
                </button>
            </div>
        </form>
    );
};

export default CategoryForm;