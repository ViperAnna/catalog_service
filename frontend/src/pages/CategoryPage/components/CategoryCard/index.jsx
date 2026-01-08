import React, {useEffect, useState} from 'react';
import {toast} from 'react-hot-toast';
import CategoryImageEditor from './CategoryImageEditor.jsx';
import CategoryHeader from './CategoryHeader.jsx';
import CategoryDescriptionEditor from './CategoryDescriptionEditor.jsx';
import CategoryStatusEditor from './CategoryStatusEditor.jsx';
import CategoryActionButtons from './CategoryActionButtons.jsx';
import CategoryStats from './CategoryStats.jsx';
import EmptyCategory from './EmptyCategory.jsx';
import {useStore} from "../../../../store/useStore.js";
import DeleteConfirmationModal from './DeleteConfirmationModal.jsx';

const CategoryCard = ({category, onSuccess}) => {
    const {updateCategory, deleteCategory} = useStore();
    const {image: initialImage} = category;
    const [isEditing, setIsEditing] = useState(false);
    const [editedName, setEditedName] = useState(category.name);
    const [editedDescription, setEditedDescription] = useState(category.description || '');
    const [editedStatus, setEditedStatus] = useState(category.status || 'active');
    const [imagePreview, setImagePreview] = useState(initialImage);
    const [imageFile, setImageFile] = useState(null);
    const [originalData, setOriginalData] = useState({
        name: category.name,
        description: category.description || '',
        status: category.status || 'active',
        image: null
    });
    const [hasChanges, setHasChanges] = useState(false);
    const [isValid, setIsValid] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);

    useEffect(() => {
        const convertImageToDataURL = async () => {
            if (category.image && !category.image.startsWith('data:')) {
                try {
                    const response = await fetch(category.image);
                    const blob = await response.blob();
                    return new Promise((resolve) => {
                        const reader = new FileReader();
                        reader.onloadend = () => resolve(reader.result);
                        reader.readAsDataURL(blob);
                    });
                } catch (error) {
                    return category.image;
                }
            }
            return category.image;
        };

        convertImageToDataURL().then(dataURL => {
            setImagePreview(dataURL);
            setOriginalData(prev => ({
                ...prev,
                image: dataURL
            }));
        });
    }, [category.image]);

    useEffect(() => {
        const nameChanged = editedName !== originalData.name;
        const descriptionChanged = editedDescription !== originalData.description;
        const statusChanged = editedStatus !== originalData.status;
        const imageChanged = imagePreview !== originalData.image;
        setHasChanges(nameChanged || descriptionChanged || statusChanged || imageChanged);
        setIsValid(editedName && editedDescription && editedStatus && imagePreview)
    }, [editedName, editedDescription, editedStatus, imagePreview, originalData]);

    const handleEditClick = () => {
        setOriginalData({
            name: editedName,
            description: editedDescription,
            status: editedStatus,
            image: imagePreview
        });
        setIsEditing(true);
    };

    const handleSaveClick = async () => {
        if (!hasChanges) return;

        setIsSubmitting(true);

        try {
            const formData = new FormData();
            formData.append('name', editedName);
            formData.append('description', editedDescription);
            formData.append('status', editedStatus);
            if (imageFile) {
                formData.append('image', imageFile);
            } else if (!imagePreview && originalData.image) {
                formData.append('image', '');
            }

            const response = await updateCategory(category.id, formData);

            if (response?.status === 200) {
                toast.success('Категория успешно обновлена!');
                onSuccess();

            } else {
                toast.error(response?.message || 'Не удалось обновить категорию');
            }

        } catch (error) {
            toast.error('Не удалось обновить категорию');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleCancelClick = () => {
        setEditedName(originalData.name);
        setEditedDescription(originalData.description);
        setEditedStatus(originalData.status);
        setImagePreview(originalData.image);
        setImageFile(null);
        setIsEditing(false);
    };

    const handleDeleteClick = () => {
        setShowDeleteModal(true);
    };

    const handleConfirmDelete = async () => {
        if (isDeleting) return;

        setIsDeleting(true);

        try {
            const response = await deleteCategory(category.id);

            if (response?.status === 204) {
                toast.success('Категория успешно удалена!');
                onSuccess();
                setShowDeleteModal(false);
            } else {
                toast.error(response?.message || 'Не удалось удалить категорию');
                setIsDeleting(false);
            }

        } catch (error) {
            toast.error('Не удалось удалить категорию');
            setIsDeleting(false);
        }
    };

    const handleCancelDelete = () => {
        setShowDeleteModal(false);
    };

    const handleKeyDown = (e, field) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (field === 'name') {
                document.getElementById('description-input')?.focus();
            }
        } else if (e.key === 'Escape') {
            handleCancelClick();
        }
    };

    const handleImageUpload = (file) => {
        setImageFile(file);
        const reader = new FileReader();
        reader.onloadend = () => {
            setImagePreview(reader.result);
        };
        reader.readAsDataURL(file);
    };

    const handleRemoveImage = () => {
        setImageFile(null);
        setImagePreview(null);
    };

    const handleRestoreImage = () => {
        setImageFile(null);
        setImagePreview(originalData.image);
    };

    return (
        <div className="bg-white rounded-2xl shadow-lg overflow-hidden mb-8 relative">
            <div className="md:flex">
                <CategoryImageEditor
                    isEditing={isEditing}
                    editedName={editedName}
                    imagePreview={imagePreview}
                    originalImage={originalData.image}
                    imageFile={imageFile}
                    onImageUpload={handleImageUpload}
                    onRemoveImage={handleRemoveImage}
                    onRestoreImage={handleRestoreImage}
                />

                <div className="md:w-3/5 p-8">
                    <CategoryActionButtons
                        isEditing={isEditing}
                        hasChanges={hasChanges}
                        isValid={isValid}
                        isSubmitting={isSubmitting}
                        onEditClick={handleEditClick}
                        onSaveClick={handleSaveClick}
                        onCancelClick={handleCancelClick}
                        onDeleteClick={handleDeleteClick}
                        isDeleting={isDeleting}
                        showDeleteButton={true}
                    />

                    <CategoryHeader
                        isEditing={isEditing}
                        editedName={editedName}
                        editedStatus={editedStatus}
                        isSubmitting={isSubmitting}
                        onNameChange={(e) => setEditedName(e.target.value)}
                        onKeyDown={handleKeyDown}
                    />

                    <CategoryStatusEditor
                        isEditing={isEditing}
                        editedStatus={editedStatus}
                        isSubmitting={isSubmitting}
                        onStatusChange={setEditedStatus}
                    />

                    <CategoryDescriptionEditor
                        isEditing={isEditing}
                        editedDescription={editedDescription}
                        isSubmitting={isSubmitting}
                        onDescriptionChange={(e) => setEditedDescription(e.target.value)}
                        onKeyDown={handleKeyDown}
                    />

                    <CategoryStats category={category}/>
                    <EmptyCategory/>
                </div>
            </div>

            <DeleteConfirmationModal
                isOpen={showDeleteModal}
                title="Удаление категории"
                message={`Вы уверены, что хотите удалить категорию "${category.name}"? Это действие нельзя отменить.`}
                onConfirm={handleConfirmDelete}
                onCancel={handleCancelDelete}
                isDeleting={isDeleting}
            />
        </div>
    );
};

export default CategoryCard;