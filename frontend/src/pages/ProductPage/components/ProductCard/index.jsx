import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'react-hot-toast';
import {useStore} from '../../../../store/useStore.js';
import ProductImageGallery from './ProductImageGallery.jsx';
import ProductImageEditor from './ProductImageEditor.jsx';
import ProductActionButtons from './ProductActionButtons.jsx';
import ProductStatusEditor from './ProductStatusEditor.jsx';
import ProductCharacteristics from './ProductCharacteristics.jsx';
import ProductTags from './ProductTags.jsx';
import ProductCategories from './ProductCategories.jsx';
import ProductStats from './ProductStats.jsx';
import DeleteConfirmationModal from './DeleteConfirmationModal.jsx';
import {urlToImageFile} from '../../../../utils/imageFile.js';

const ProductCard = ({product, onSuccess}) => {
    const navigate = useNavigate();
    const {updateProduct, deleteProduct, categories, fetchCategories} = useStore();

    const [isEditing, setIsEditing] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);

    const [editedName, setEditedName] = useState(product.name);
    const [editedBrand, setEditedBrand] = useState(product.brand || '');
    const [editedPrice, setEditedPrice] = useState(product.price?.toString() || '');
    const [editedDescription, setEditedDescription] = useState(product.description || '');
    const [editedStatus, setEditedStatus] = useState(product.status);
    const [editedTags, setEditedTags] = useState(product.tag || []);
    const [editedCharacteristic, setEditedCharacteristic] = useState(
        product.characteristic || {attributes: {}}
    );
    const [editedCategoryIds, setEditedCategoryIds] = useState(
        product.categories?.map(c => c.id) || []
    );

    const [keptImages, setKeptImages] = useState(product.imagesUrl || []);
    const [newImageFiles, setNewImageFiles] = useState([]);
    const [newImagePreviews, setNewImagePreviews] = useState([]);
    const [uploadProgress, setUploadProgress] = useState(0);

    const [original, setOriginal] = useState(null);

    useEffect(() => {
        if (categories.length === 0) fetchCategories();
    }, []);

    const hasChanges = original && (
        editedName !== original.name ||
        editedBrand !== original.brand ||
        editedPrice !== original.price ||
        editedDescription !== original.description ||
        editedStatus !== original.status ||
        JSON.stringify(editedTags) !== JSON.stringify(original.tags) ||
        JSON.stringify(editedCharacteristic) !== JSON.stringify(original.characteristic) ||
        JSON.stringify(editedCategoryIds.sort()) !== JSON.stringify(original.categoryIds.sort()) ||
        JSON.stringify(keptImages) !== JSON.stringify(original.images) ||
        newImageFiles.length > 0
    );

    const isValid =
        editedName.trim() &&
        editedBrand.trim() &&
        editedDescription.trim() &&
        editedPrice !== '' &&
        Number(editedPrice) > 0 &&
        editedTags.length > 0 &&
        editedCategoryIds.length > 0 &&
        (keptImages.length + newImageFiles.length) > 0;

    const handleEditClick = () => {
        const currentImages = product.imagesUrl || [];
        setKeptImages(currentImages);
        setNewImageFiles([]);
        setNewImagePreviews([]);
        setOriginal({
            name: editedName,
            brand: editedBrand,
            price: editedPrice,
            description: editedDescription,
            status: editedStatus,
            tags: [...editedTags],
            characteristic: JSON.parse(JSON.stringify(editedCharacteristic)),
            categoryIds: [...editedCategoryIds],
            images: [...currentImages],
        });
        setIsEditing(true);
    };

    const handleCancelClick = () => {
        if (original) {
            setEditedName(original.name);
            setEditedBrand(original.brand);
            setEditedPrice(original.price);
            setEditedDescription(original.description);
            setEditedStatus(original.status);
            setEditedTags(original.tags);
            setEditedCharacteristic(original.characteristic);
            setEditedCategoryIds(original.categoryIds);
            setKeptImages(original.images);
        }
        setNewImageFiles([]);
        setNewImagePreviews([]);
        setIsEditing(false);
    };

    const handleAddImages = (files) => {
        setNewImageFiles(prev => [...prev, ...files]);
        files.forEach(file => {
            const reader = new FileReader();
            reader.onloadend = () => setNewImagePreviews(prev => [...prev, reader.result]);
            reader.readAsDataURL(file);
        });
    };

    const handleRemoveKept = (idx) => {
        setKeptImages(prev => prev.filter((_, i) => i !== idx));
    };

    const handleRemoveNew = (idx) => {
        setNewImageFiles(prev => prev.filter((_, i) => i !== idx));
        setNewImagePreviews(prev => prev.filter((_, i) => i !== idx));
    };

    const handleSaveClick = async () => {
        if (!hasChanges) return;
        setIsSubmitting(true);
        setUploadProgress(0);
        try {
            const fd = new FormData();
            fd.append('name', editedName.trim());
            fd.append('description', editedDescription.trim());
            fd.append('brand', editedBrand.trim());
            fd.append('price', editedPrice);
            fd.append('status', editedStatus);
            editedCategoryIds.forEach(id => fd.append('categories', id));
            editedTags.forEach(tag => fd.append('tag', tag));

            const attrs = editedCharacteristic?.attributes || {};
            const validAttrs = Object.entries(attrs).filter(([k, v]) => k.trim() && String(v).trim());
            if (validAttrs.length > 0) {
                validAttrs.forEach(([k, v]) => fd.append(`characteristic.attributes[${k.trim()}]`, String(v).trim()));
            } else {
                fd.append('characteristic.attributes[—]', '—');
            }

            for (let i = 0; i < keptImages.length; i++) {
                const file = await urlToImageFile(keptImages[i], `image_${i}.jpg`);
                fd.append('images', file);
            }
            newImageFiles.forEach(file => fd.append('images', file));

            const response = await updateProduct(product.id, fd, {
                onUploadProgress: (ev) => {
                    if (ev.total) setUploadProgress(Math.round((ev.loaded * 100) / ev.total));
                },
            });
            if (response?.status === 201 || response?.status === 200) {
                toast.success('Товар успешно обновлён!');
                setIsEditing(false);
                onSuccess();
            } else {
                toast.error(response?.data?.message || 'Не удалось обновить товар');
            }
        } catch (error) {
            toast.error(error?.message || 'Не удалось обновить товар');
        } finally {
            setIsSubmitting(false);
            setUploadProgress(0);
        }
    };

    const handleConfirmDelete = async () => {
        if (isDeleting) return;
        setIsDeleting(true);
        try {
            const response = await deleteProduct(product.id);
            if (response?.status === 204 || response?.status === 200) {
                toast.success('Товар успешно удалён!');
                navigate('/products');
            } else {
                toast.error('Не удалось удалить товар');
                setIsDeleting(false);
            }
        } catch (error) {
            toast.error(error?.message || 'Не удалось удалить товар');
            setIsDeleting(false);
        }
    };

    const handleCategoryToggle = (id) => {
        setEditedCategoryIds(prev =>
            prev.includes(id) ? prev.filter(c => c !== id) : [...prev, id]
        );
    };

    return (
        <div className="bg-white rounded-2xl shadow-lg overflow-hidden mb-8">
            <div className="md:flex">
                {isEditing ? (
                    <ProductImageEditor
                        keptImages={keptImages}
                        newPreviews={newImagePreviews}
                        onRemoveKept={handleRemoveKept}
                        onRemoveNew={handleRemoveNew}
                        onAddFiles={handleAddImages}
                        isSubmitting={isSubmitting}
                        uploadProgress={uploadProgress}
                    />
                ) : (
                    <ProductImageGallery images={product.imagesUrl} productName={product.name}/>
                )}

                <div className="md:w-3/5 p-8">
                    <ProductActionButtons
                        isEditing={isEditing}
                        hasChanges={hasChanges}
                        isValid={isValid}
                        isSubmitting={isSubmitting}
                        isDeleting={isDeleting}
                        onEditClick={handleEditClick}
                        onSaveClick={handleSaveClick}
                        onCancelClick={handleCancelClick}
                        onDeleteClick={() => setShowDeleteModal(true)}
                    />

                    <div className="mb-2">
                        {isEditing ? (
                            <input
                                type="text"
                                value={editedName}
                                onChange={e => setEditedName(e.target.value)}
                                disabled={isSubmitting}
                                className="w-full text-2xl font-bold text-gray-900 border-b-2 border-emerald-400 focus:outline-none pb-1 bg-transparent disabled:opacity-50"
                            />
                        ) : (
                            <h1 className="text-2xl font-bold text-gray-900">{editedName}</h1>
                        )}
                    </div>

                    <div className="mb-4">
                        {isEditing ? (
                            <input
                                type="text"
                                value={editedBrand}
                                onChange={e => setEditedBrand(e.target.value)}
                                disabled={isSubmitting}
                                placeholder="Бренд"
                                className="text-gray-500 border-b border-gray-300 focus:border-emerald-400 focus:outline-none bg-transparent w-full disabled:opacity-50"
                            />
                        ) : (
                            <p className="text-gray-500 font-medium">{editedBrand}</p>
                        )}
                    </div>

                    <div className="mb-4">
                        {isEditing ? (
                            <div className="flex items-center gap-2">
                                <input
                                    type="number"
                                    value={editedPrice}
                                    onChange={e => setEditedPrice(e.target.value)}
                                    disabled={isSubmitting}
                                    min="0"
                                    step="0.01"
                                    className="text-3xl font-bold text-emerald-600 border-b-2 border-emerald-400 focus:outline-none bg-transparent w-40 disabled:opacity-50"
                                />
                                <span className="text-3xl font-bold text-emerald-600">₽</span>
                            </div>
                        ) : (
                            <p className="text-3xl font-bold text-emerald-600">
                                {Number(product.price).toLocaleString('ru-RU')} ₽
                            </p>
                        )}
                    </div>

                    <ProductStatusEditor
                        isEditing={isEditing}
                        editedStatus={editedStatus}
                        isSubmitting={isSubmitting}
                        onStatusChange={setEditedStatus}
                    />

                    <div className="mb-6">
                        <h4 className="text-sm font-semibold text-gray-700 mb-2">Описание</h4>
                        {isEditing ? (
                            <>
                                <textarea
                                    value={editedDescription}
                                    onChange={e => setEditedDescription(e.target.value)}
                                    disabled={isSubmitting}
                                    rows={3}
                                    maxLength={500}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-gray-700 focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none resize-none disabled:opacity-50"
                                />
                                <p className="text-xs text-gray-400 text-right mt-1">{editedDescription.length}/500</p>
                            </>
                        ) : (
                            <p className="text-gray-600 leading-relaxed">{editedDescription}</p>
                        )}
                    </div>

                    <ProductCategories
                        isEditing={isEditing}
                        selectedIds={editedCategoryIds}
                        isSubmitting={isSubmitting}
                        onToggle={handleCategoryToggle}
                        displayCategories={product.categories}
                    />

                    <ProductTags
                        isEditing={isEditing}
                        tags={editedTags}
                        isSubmitting={isSubmitting}
                        onChange={setEditedTags}
                    />

                    <ProductCharacteristics
                        isEditing={isEditing}
                        characteristics={editedCharacteristic}
                        isSubmitting={isSubmitting}
                        onChange={setEditedCharacteristic}
                    />

                    <ProductStats product={product}/>
                </div>
            </div>

            <DeleteConfirmationModal
                isOpen={showDeleteModal}
                title="Удаление товара"
                message={`Вы уверены, что хотите удалить товар «${product.name}»? Это действие нельзя отменить.`}
                onConfirm={handleConfirmDelete}
                onCancel={() => setShowDeleteModal(false)}
                isDeleting={isDeleting}
            />
        </div>
    );
};

export default ProductCard;
