export const urlToImageFile = async (url, fallbackName) => {
    const response = await fetch(url);
    if (!response.ok) throw new Error('Не удалось загрузить изображение');
    const blob = await response.blob();
    const type = blob.type && blob.type.startsWith('image/') ? blob.type : 'image/jpeg';
    const name = fallbackName || `image_${Date.now()}.jpg`;
    return new File([blob], name, {type});
};
