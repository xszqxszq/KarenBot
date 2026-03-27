document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('[style]').forEach(el => {
        const styleAttr = el.getAttribute('style');
        const strokeMatch = styleAttr.match(/text-stroke:\s*([^;]+)/i);
        
        if (strokeMatch) {
            const strokeValue = strokeMatch[1].trim();
            const widthMatch = strokeValue.match(/([0-9.]+)px/i);
            
            if (widthMatch) {
                const requestedWidth = parseFloat(widthMatch[1]);
                const color = strokeValue.replace(/[0-9.]+px/i, '').trim();
                
                el.style.webkitTextStroke = `${requestedWidth}px ${color}`;
                el.style.paintOrder = "stroke fill";

                const shadowMatch = styleAttr.match(/text-shadow:\s*([^;]+)/i);
                if (shadowMatch) {
                    el.style.textShadow = "none"; 
                    el.style.filter = `drop-shadow(${shadowMatch[1].trim()})`; 
                }
            }
        }
    });
    document.querySelectorAll('[style*="background-image"]').forEach(el => {
        const styleStr = el.getAttribute('style') || '';
        
        const bgMatch = styleStr.match(/background-image:\s*url\(['"]?(.*?)['"]?\)/);
        const maskMatch = styleStr.match(/(?:-webkit-)?mask-image:\s*url\(['"]?(.*?)['"]?\)/);
        const opacityMatch = styleStr.match(/background-opacity:\s*([0-9.]+)/);
        
        if (!bgMatch) return; 
        
        if (!maskMatch && !opacityMatch) return;

        const bgUrl = bgMatch[1];
        const maskUrl = maskMatch ? maskMatch[1] : null;
        const bgOpacity = opacityMatch ? parseFloat(opacityMatch[1]) : 1.0;
        
        if (window.getComputedStyle(el).position === 'static') {
            el.style.position = 'relative';
        }
        if (window.getComputedStyle(el).zIndex === 'auto') {
            el.style.zIndex = '0';
        }
        
        const canvas = document.createElement('canvas');
        canvas.width = el.offsetWidth || parseFloat(el.style.width);
        canvas.height = el.offsetHeight || parseFloat(el.style.height);
        canvas.style.position = 'absolute';
        canvas.style.left = '0';
        canvas.style.top = '0';
        canvas.style.zIndex = '-1';
        canvas.style.pointerEvents = 'none';

        const ctx = canvas.getContext('2d');
        const bgImg = new Image();
        let maskImg = null;
        
        let loadedCount = 0;
        const targetCount = maskUrl ? 2 : 1;
        
        const onImageLoad = () => {
            loadedCount++;
            if (loadedCount === targetCount) { 
                ctx.globalAlpha = bgOpacity; 
                
                const bgSize = styleStr.match(/background-size:\s*(cover|contain|100%\s*100%)/)?.[1] || 'auto';
                
                let dx = 0, dy = 0, drawW = canvas.width, drawH = canvas.height;
                
                if (bgSize === 'cover') {
                    const scale = Math.max(canvas.width / bgImg.width, canvas.height / bgImg.height);
                    drawW = bgImg.width * scale;
                    drawH = bgImg.height * scale;
                    dx = (canvas.width - drawW) / 2;
                    dy = (canvas.height - drawH) / 2;
                } else if (bgSize === 'contain') {
                    const scale = Math.min(canvas.width / bgImg.width, canvas.height / bgImg.height);
                    drawW = bgImg.width * scale;
                    drawH = bgImg.height * scale;
                    dx = (canvas.width - drawW) / 2;
                    dy = (canvas.height - drawH) / 2;
                } else if (bgSize === '100% 100%') {
                    drawW = canvas.width;
                    drawH = canvas.height;
                } else {
                    drawW = bgImg.width;
                    drawH = bgImg.height;
                    if (styleStr.includes('center')) dx = (canvas.width - drawW) / 2;
                }
                
                ctx.drawImage(bgImg, dx, dy, drawW, drawH);
                
                if (maskUrl) {
                    ctx.globalAlpha = 1.0;
                    ctx.globalCompositeOperation = 'destination-in';
                    ctx.drawImage(maskImg, 0, 0, canvas.width, canvas.height);
                }
                
                el.style.backgroundImage = 'none';
                if (maskUrl) {
                    el.style.maskImage = 'none';
                    el.style.webkitMaskImage = 'none';
                }
                
                el.insertBefore(canvas, el.firstChild);
            }
        };
        
        bgImg.src = bgUrl;
        bgImg.onload = onImageLoad;
        
        if (maskUrl) {
            maskImg = new Image();
            maskImg.src = maskUrl;
            maskImg.onload = onImageLoad;
        }
    });
    document.querySelectorAll('img[style*="mask-image"]').forEach(img => {
        const styleStr = img.getAttribute('style');
        
        const maskMatch = styleStr.match(/(?:-webkit-)?mask-image:\s*url\(['"]?(.*?)['"]?\)/);
        if (!maskMatch) return;
        
        const maskUrl = maskMatch[1];
        const bgUrl = img.getAttribute('src');
        
        const canvas = document.createElement('canvas');
        canvas.width = img.offsetWidth || parseInt(img.style.width);
        canvas.height = img.offsetHeight || parseInt(img.style.height);
        
        canvas.style.cssText = styleStr;
        canvas.style.maskImage = 'none';
        canvas.style.webkitMaskImage = 'none';
        
        img.style.display = 'none';
        img.parentNode.insertBefore(canvas, img);

        const ctx = canvas.getContext('2d');
        const baseImg = new Image();
        const maskImg = new Image();
        
        let loadedCount = 0;
        const onImageLoad = () => {
            loadedCount++;
            if (loadedCount === 2) {
                let dx = 0, dy = 0, drawW = canvas.width, drawH = canvas.height;
                if (img.style.objectFit === 'cover') {
                    const scale = Math.max(canvas.width / baseImg.width, canvas.height / baseImg.height);
                    drawW = baseImg.width * scale;
                    drawH = baseImg.height * scale;
                    dx = (canvas.width - drawW) / 2;
                    dy = (canvas.height - drawH) / 2;
                }
                
                ctx.drawImage(baseImg, dx, dy, drawW, drawH);
                
                ctx.globalCompositeOperation = 'destination-in';
                
                ctx.drawImage(maskImg, 0, 0, canvas.width, canvas.height);
            }
        };
        
        baseImg.src = bgUrl;
        maskImg.src = maskUrl;
        baseImg.onload = onImageLoad;
        maskImg.onload = onImageLoad;
    });
    document.querySelectorAll('span[style*="min-font-size"]').forEach(span => {
        const styleStr = span.getAttribute('style') || '';
        const minSizeMatch = styleStr.match(/min-font-size:\s*([0-9.]+)px/);
        if (!minSizeMatch) return;

        const minSize = parseFloat(minSizeMatch[1]);
        let currentSize = parseFloat(window.getComputedStyle(span).fontSize);

        const explicitWidthMatch = styleStr.match(/width:\s*([0-9.]+)px/);
        const constraintWidth = explicitWidthMatch ? parseFloat(explicitWidthMatch[1]) : span.parentElement.clientWidth;

        const originalWhiteSpace = span.style.whiteSpace;
        span.style.whiteSpace = 'nowrap';
        const originalDisplay = span.style.display;
        span.style.display = 'inline-block';
        
        while (span.scrollWidth > constraintWidth && currentSize > minSize) {
            currentSize -= 1;
            span.style.fontSize = currentSize + 'px';
        }

        span.style.whiteSpace = originalWhiteSpace;
        span.style.display = originalDisplay;
    });
});